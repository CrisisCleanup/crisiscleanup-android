package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.UuidGenerator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.split
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.database.model.asEntities
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.asLookup
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.SavedWorksiteChange
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteChangeArchiveAction
import com.crisiscleanup.core.model.data.WorksiteChangeSerializer
import com.crisiscleanup.core.model.data.WorksiteSyncResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

class WorksiteChangeDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
    private val uuidGenerator: UuidGenerator,
    private val changeSerializer: WorksiteChangeSerializer,
    private val appVersionProvider: AppVersionProvider,
    @Logger(CrisisCleanupLoggers.App) private val appLogger: AppLogger,
    private val syncLogger: SyncLogger,
) {
    private fun getLocalNetworkIdMap(worksite: Worksite): IdNetworkIdMaps {
        if (worksite.isNew || worksite.networkId < 0) {
            return IdNetworkIdMaps()
        }

        val flagIdMap = db.worksiteFlagDao().getNetworkedIdMap(worksite.id).asLookup()
        val noteIdMap = db.worksiteNoteDao().getNetworkedIdMap(worksite.id).asLookup()
        val workTypeIdMap = db.workTypeDao().getNetworkedIdMap(worksite.id).asLookup()
        return IdNetworkIdMaps(
            flag = flagIdMap,
            note = noteIdMap,
            workType = workTypeIdMap,
        )
    }

    suspend fun saveChange(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        primaryWorkType: WorkType,
        organizationId: Long,
        localModifiedAt: Instant = Clock.System.now(),
    ): Long {
        var worksiteId = worksiteChange.id

        if (worksiteStart == worksiteChange) {
            return worksiteId
        }

        val logPostfix = localModifiedAt.epochSeconds.toString()
        syncLogger.type = if (worksiteChange.isNew) "worksite-new-$logPostfix"
        else "worksite-update-$worksiteId-$logPostfix"

        db.withTransaction {
            try {
                val worksiteDao = db.worksiteDao()

                val idMapping = getLocalNetworkIdMap(worksiteChange)

                val changeEntities = worksiteChange.asEntities(
                    uuidGenerator,
                    primaryWorkType,
                    idMapping.flag,
                    idMapping.note,
                    idMapping.workType,
                )

                val flagDao = db.worksiteFlagDao()
                val formDataDao = db.worksiteFormDataDao()
                val noteDao = db.worksiteNoteDao()
                val workTypeDao = db.workTypeDao()

                var flags = changeEntities.flags
                var formData = changeEntities.formData
                var insertNotes = changeEntities.notes.filter { it.id <= 0 }
                var workTypes = changeEntities.workTypes

                if (worksiteChange.isNew) {
                    val rootEntity = WorksiteRootEntity(
                        id = 0,
                        syncUuid = uuidGenerator.uuid(),
                        localModifiedAt = localModifiedAt,
                        syncedAt = Instant.fromEpochSeconds(0),
                        localGlobalUuid = uuidGenerator.uuid(),
                        isLocalModified = true,
                        syncAttempt = 0,
                        networkId = -1,
                        incidentId = changeEntities.core.incidentId,
                    )

                    worksiteId = worksiteDao.insertRoot(rootEntity)

                    val worksiteEntity = changeEntities.core.copy(id = worksiteId)
                    worksiteDao.insert(worksiteEntity)

                    syncLogger.log("Saved new worksite: $worksiteId")

                    flags = flags.map { it.copy(worksiteId = worksiteId) }
                    formData = formData.map { it.copy(worksiteId = worksiteId) }
                    insertNotes = insertNotes.map { it.copy(worksiteId = worksiteId) }
                    workTypes = workTypes.map { it.copy(worksiteId = worksiteId) }
                } else {
                    val core = changeEntities.core
                    worksiteDao.updateRoot(
                        core.id,
                        uuidGenerator.uuid(),
                        localModifiedAt,
                    )

                    worksiteDao.update(changeEntities.core)

                    flagDao.deleteUnspecified(worksiteId, flags.map(WorksiteFlagEntity::id).toSet())
                    formDataDao.deleteUnspecifiedKeys(
                        worksiteId,
                        formData.map(WorksiteFormDataEntity::fieldKey).toSet(),
                    )
                    workTypeDao.deleteUnspecified(
                        worksiteId,
                        workTypes.map(WorkTypeEntity::workType).toSet(),
                    )
                }

                var worksiteUpdatedIds = worksiteChange.copy(id = worksiteId)

                flags
                    .split { it.id <= 0 }
                    .also { (inserts, updates) ->
                        val insertIds = flagDao.insertIgnore(inserts)
                        val unsyncedLookup = inserts
                            .mapIndexedNotNull { index, flag ->
                                val id = insertIds[index]
                                if (id > 0) Pair(flag.reasonT, id)
                                else null
                            }
                            .associate { it.first to it.second }
                        if (unsyncedLookup.isNotEmpty()) {
                            val updatedIds = worksiteUpdatedIds.flags?.map {
                                val localId = unsyncedLookup[it.reasonT]
                                if (localId == null || it.id > 0) it
                                else it.copy(id = localId)
                            }
                            worksiteUpdatedIds = worksiteUpdatedIds.copy(flags = updatedIds)
                        }

                        flagDao.update(updates)

                        syncLogger.log("Flags. Inserted ${inserts.size}. Updated ${updates.size}")
                    }

                formDataDao.upsert(formData)
                syncLogger.log("Form data. Upserted ${formData.size}.")

                if (insertNotes.isNotEmpty()) {
                    val insertIds = noteDao.insertIgnore(insertNotes)
                    var insertedIndex = 0
                    worksiteUpdatedIds = worksiteUpdatedIds.copy(
                        notes = worksiteUpdatedIds.notes.map { note ->
                            if (note.id <= 0) {
                                if (insertedIndex < insertNotes.size &&
                                    note.note == insertNotes[insertedIndex].note
                                ) {
                                    val insertId = insertIds[insertedIndex]
                                    insertedIndex++
                                    return@map note.copy(id = insertId)
                                }
                            }
                            note
                        }
                    )
                    syncLogger.log("Notes. Inserted ${insertNotes.size}.")
                }

                workTypes.split { it.id <= 0 }.also { (inserts, updates) ->
                    val insertIds = workTypeDao.insertIgnore(inserts)
                    val unsyncedLookup = inserts
                        .mapIndexedNotNull { index, flag ->
                            val id = insertIds[index]
                            if (id > 0) Pair(flag.workType, id)
                            else null
                        }
                        .associate { it.first to it.second }
                    if (unsyncedLookup.isNotEmpty()) {
                        val updatedIds = worksiteUpdatedIds.workTypes.map {
                            val localId = unsyncedLookup[it.workTypeLiteral]
                            if (localId == null || it.id > 0) it
                            else it.copy(id = localId)
                        }
                        worksiteUpdatedIds = worksiteUpdatedIds.copy(workTypes = updatedIds)
                    }

                    workTypeDao.update(updates)

                    syncLogger.log("Work types. Inserted ${inserts.size}. Updated ${updates.size}")
                }

                saveWorksiteChange(
                    worksiteStart,
                    worksiteUpdatedIds,
                    idMapping,
                    appVersionProvider.versionCode,
                    organizationId,
                )
            } catch (e: Exception) {
                appLogger.logException(e)
                throw e
            } finally {
                syncLogger.flush()
            }
        }

        return worksiteId
    }

    private suspend fun saveWorkTypeTransfer(
        worksite: Worksite,
        transferType: String,
        localModifiedAt: Instant,
        saveBlock: suspend (Map<String, WorkType>) -> Unit,
    ) {
        val logPostfix = localModifiedAt.epochSeconds.toString()
        syncLogger.type = "worksite-$transferType-${worksite.id}-$logPostfix"

        val workTypeLookup = worksite.workTypes.associateBy(WorkType::workTypeLiteral)
        db.withTransaction {
            try {
                saveBlock(workTypeLookup)
            } catch (e: Exception) {
                appLogger.logException(e)
                throw e
            } finally {
                syncLogger.flush()
            }
        }
    }

    private fun saveWorksiteTransferChange(
        worksite: Worksite,
        organizationId: Long,
        requestReason: String = "",
        requests: List<String> = emptyList(),
        releaseReason: String = "",
        releases: List<String> = emptyList(),
    ) {
        val (flagIdMap, noteIdMap, workTypeIdMap) = getLocalNetworkIdMap(worksite)
        val (changeVersion, serializedChange) = changeSerializer.serialize(
            EmptyWorksite,
            worksite,
            flagIdMap,
            noteIdMap,
            workTypeIdMap,
            requestReason,
            requests,
            releaseReason,
            releases,
        )
        val changeEntity = WorksiteChangeEntity(
            0,
            appVersionProvider.versionCode,
            organizationId,
            worksite.id,
            uuidGenerator.uuid(),
            changeVersion,
            serializedChange,
        )
        db.worksiteChangeDao().insert(changeEntity)
    }

    suspend fun saveWorkTypeRequests(
        worksite: Worksite,
        organizationId: Long,
        reason: String,
        requests: List<String>,
        localModifiedAt: Instant = Clock.System.now(),
    ) {
        if (worksite.isNew ||
            organizationId <= 0 ||
            reason.isBlank() ||
            requests.isEmpty()
        ) {
            appLogger.logDebug("Not saving work type requests. Invalid data.")
            return
        }

        saveWorkTypeTransfer(
            worksite,
            "request",
            localModifiedAt,
        ) { workTypeLookup ->
            val requestEntities = requests.mapNotNull {
                workTypeLookup[it]?.let { workType ->
                    if (workType.orgClaim == null) null
                    else WorkTypeTransferRequestEntity(
                        0,
                        networkId = -1,
                        worksiteId = worksite.id,
                        workType = it,
                        reason = reason,
                        byOrg = organizationId,
                        toOrg = workType.orgClaim!!,
                        createdAt = localModifiedAt,
                    )
                }
            }

            if (requestEntities.isNotEmpty()) {
                db.workTypeTransferRequestDao().insertIgnore(requestEntities)
                val requestedWorkTypes =
                    requestEntities.map(WorkTypeTransferRequestEntity::workType)

                syncLogger.log(
                    "Requested ${requestEntities.size} work types.",
                    "$requestedWorkTypes",
                )

                saveWorksiteTransferChange(
                    worksite,
                    organizationId,
                    requestReason = reason,
                    requests = requestedWorkTypes,
                )
            }
        }
    }

    suspend fun saveWorkTypeReleases(
        worksite: Worksite,
        organizationId: Long,
        reason: String,
        releases: List<String>,
        localModifiedAt: Instant = Clock.System.now(),
    ) {
        if (worksite.isNew ||
            organizationId <= 0 ||
            reason.isBlank() ||
            releases.isEmpty()
        ) {
            appLogger.logDebug("Not saving work type releases. Invalid data.")
            return
        }

        saveWorkTypeTransfer(
            worksite,
            "release",
            localModifiedAt,
        ) { workTypeLookup ->
            val releaseWorkTypes = releases.filter {
                workTypeLookup[it]?.let { workType -> workType.orgClaim != null } ?: false
            }

            if (releaseWorkTypes.isNotEmpty()) {
                val worksiteId = worksite.id
                val workTypeDao = db.workTypeDao()
                workTypeDao.deleteSpecified(worksiteId, releaseWorkTypes.toSet())

                val workTypeStatusMap =
                    worksite.workTypes.associate { it.workTypeLiteral to it.statusLiteral }

                val workTypeEntities = releaseWorkTypes.map {
                    val statusLiteral =
                        workTypeStatusMap[it] ?: WorkTypeStatus.OpenUnassigned.literal
                    WorkTypeEntity(
                        0,
                        networkId = -1,
                        worksiteId = worksiteId,
                        createdAt = localModifiedAt,
                        status = statusLiteral,
                        workType = it,
                    )
                }
                val insertIds = workTypeDao.insertIgnore(workTypeEntities)

                val workTypeInsertIdLookup = workTypeEntities
                    .mapIndexed { index, workType -> Pair(workType.workType, insertIds[index]) }
                    .associate { it.first to it.second }
                val updatedWorkTypes = worksite.workTypes.map { workType ->
                    val workTypeLiteral = workType.workTypeLiteral
                    val statusLiteral =
                        workTypeStatusMap[workTypeLiteral] ?: WorkTypeStatus.OpenUnassigned.literal
                    workTypeInsertIdLookup[workTypeLiteral]?.let { insertId ->
                        WorkType(
                            id = insertId,
                            createdAt = localModifiedAt,
                            statusLiteral = statusLiteral,
                            workTypeLiteral = workTypeLiteral,
                        )
                    } ?: workType
                }
                val updatedWorksite = worksite.copy(
                    keyWorkType = worksite.keyWorkType?.let { keyWorkType ->
                        updatedWorkTypes.find { it.workTypeLiteral == keyWorkType.workTypeLiteral }
                            ?: keyWorkType
                    },
                    workTypes = updatedWorkTypes,
                )

                syncLogger.log(
                    "Released ${releaseWorkTypes.size} work types",
                    "$releaseWorkTypes",
                )

                saveWorksiteTransferChange(
                    updatedWorksite,
                    organizationId,
                    releaseReason = reason,
                    releases = releaseWorkTypes,
                )
            }
        }
    }

    suspend fun saveDeletePhoto(fileId: Long, organizationId: Long) = db.withTransaction {
        db.networkFileDao().getWorksiteFromFile(fileId)?.let { (worksiteId) ->
            val localImageDaoPlus = LocalImageDaoPlus(db)
            localImageDaoPlus.deleteNetworkImage(fileId)

            val (changeVersion, serializedChange) = changeSerializer.serialize(
                EmptyWorksite,
                EmptyWorksite.copy(id = worksiteId),
                isPhotoChange = true,
            )
            val changeEntity = WorksiteChangeEntity(
                0,
                appVersionProvider.versionCode,
                organizationId,
                worksiteId,
                "",
                changeVersion,
                serializedChange,
            )
            db.worksiteChangeDao().insert(changeEntity)
            return@withTransaction worksiteId
        }

        return@withTransaction -1
    }

    private fun saveWorksiteChange(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        idMapping: IdNetworkIdMaps,
        appVersion: Long,
        organizationId: Long,
    ) {
        val (changeVersion, serializedChange) = changeSerializer.serialize(
            worksiteStart,
            worksiteChange,
            flagIdLookup = idMapping.flag,
            noteIdLookup = idMapping.note,
            workTypeIdLookup = idMapping.workType,
        )
        val changeEntity = WorksiteChangeEntity(
            0,
            appVersion,
            organizationId,
            worksiteChange.id,
            uuidGenerator.uuid(),
            changeVersion,
            serializedChange,
        )
        db.worksiteChangeDao().insert(changeEntity)
    }

    suspend fun updateSyncIds(
        worksiteId: Long,
        organizationId: Long,
        ids: WorksiteSyncResult.ChangeIds,
    ) {
        db.withTransaction {
            val worksiteDao = db.worksiteDao()
            val networkId = ids.networkWorksiteId
            if (networkId > 0) {
                worksiteDao.updateRootNetworkId(worksiteId, ids.networkWorksiteId)
                worksiteDao.updateWorksiteNetworkId(worksiteId, ids.networkWorksiteId)
            }

            val flagDao = db.worksiteFlagDao()
            val flagIds = ids.flagIdMap.filter { it.value > 0 }
            for ((key, value) in flagIds) {
                flagDao.updateNetworkId(key, value)
            }

            val notesDao = db.worksiteNoteDao()
            val noteIds = ids.noteIdMap.filter { it.value > 0 }
            for ((key, value) in noteIds) {
                notesDao.updateNetworkId(key, value)
            }

            val workTypeDao = db.workTypeDao()
            val workTypeIds = ids.workTypeIdMap.filter { it.value > 0 }
            for ((key, value) in workTypeIds) {
                workTypeDao.updateNetworkId(key, value)
            }
            val workTypeKeyIds = ids.workTypeKeyMap.filter { it.value > 0 }
            for ((key, value) in workTypeKeyIds) {
                workTypeDao.updateNetworkId(worksiteId, key, value)
            }

            val workTypeRequestIds = ids.workTypeRequestIdMap.filter { it.value > 0 }
            val requestsDao = db.workTypeTransferRequestDao()
            for ((key, value) in workTypeRequestIds) {
                requestsDao.updateNetworkId(worksiteId, key, organizationId, value)
            }
        }
    }

    suspend fun updateSyncChanges(
        worksiteId: Long,
        changeResults: Collection<WorksiteSyncResult.ChangeResult>,
        maxSyncAttempts: Int = 3,
    ) {
        db.withTransaction {
            val worksiteChangeDao = db.worksiteChangeDao()
            changeResults.forEach { result ->
                if (result.isFail) {
                    worksiteChangeDao.updateSyncAttempt(result.id)
                } else if (result.isSuccessful || result.isPartiallySuccessful) {
                    val action = if (result.isSuccessful) WorksiteChangeArchiveAction.Synced
                    else WorksiteChangeArchiveAction.PartiallySynced
                    worksiteChangeDao.updateAction(result.id, action.literal)
                }
            }
        }

        db.withTransaction {
            val worksiteChangeDao = db.worksiteChangeDao()
            val syncChanges = worksiteChangeDao.getOrdered(worksiteId)
                .map { it.asExternalModel(maxSyncAttempts) }

            if (syncChanges.isNotEmpty()) {
                var deleteIds = emptySet<Long>()
                if (syncChanges.last().isSynced) {
                    deleteIds = syncChanges.map(SavedWorksiteChange::id).toSet()
                } else {
                    var lastSyncedIndex = syncChanges.size
                    for (index in syncChanges.size - 1 downTo 0) {
                        if (syncChanges[index].isSynced) {
                            lastSyncedIndex = index
                            break
                        }
                    }

                    if (lastSyncedIndex < syncChanges.size) {
                        deleteIds = syncChanges
                            .subList(0, lastSyncedIndex + 1)
                            .map(SavedWorksiteChange::id)
                            .toSet()
                    }
                }

                if (deleteIds.isNotEmpty()) {
                    worksiteChangeDao.delete(deleteIds)
                }
            }
        }
    }
}

private data class IdNetworkIdMaps(
    val flag: Map<Long, Long> = emptyMap(),
    val note: Map<Long, Long> = emptyMap(),
    val workType: Map<Long, Long> = emptyMap(),
)