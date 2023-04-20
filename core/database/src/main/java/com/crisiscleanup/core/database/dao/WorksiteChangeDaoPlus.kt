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
import com.crisiscleanup.core.database.model.*
import com.crisiscleanup.core.model.data.*
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

        val flagIdMap = db.worksiteFlagDao().getNetworkedIdMap(worksite.id)
            .associate { it.id to it.networkId }

        val noteIdMap = db.worksiteNoteDao().getNetworkedIdMap(worksite.id)
            .associate { it.id to it.networkId }

        val workTypeIdMap = db.workTypeDao().getNetworkedIdMap(worksite.id)
            .associate { it.id to it.networkId }

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

        syncLogger.type = if (worksiteChange.isNew) "worksite-new"
        else "worksite-update-$worksiteId"

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

                    flagDao.deleteUnspecified(worksiteId, idMapping.flag.keys)
                    formDataDao.deleteUnspecifiedKeys(
                        worksiteId,
                        formData.map(WorksiteFormDataEntity::fieldKey).toSet(),
                    )
                    workTypeDao.deleteUnspecified(worksiteId, idMapping.workType.keys)
                }

                flags
                    .split { it.id <= 0 }
                    .also { (inserts, updates) ->
                        flagDao.insertIgnore(inserts)
                        flagDao.update(updates)

                        syncLogger.log("Flags. Inserted ${inserts.size}. Updated ${updates.size}")
                    }

                formDataDao.upsert(formData)
                syncLogger.log("Form data. Upserted ${formData.size}.")

                noteDao.insertIgnore(insertNotes)
                syncLogger.log("Notes. Inserted ${insertNotes.size}.")

                workTypes.split { it.id <= 0 }.also { (inserts, updates) ->
                    workTypeDao.insertIgnore(inserts)
                    workTypeDao.update(updates)

                    syncLogger.log("Work types. Inserted ${inserts.size}. Updated ${updates.size}")
                }

                saveWorksiteChange(
                    worksiteStart,
                    worksiteChange.copy(id = worksiteId),
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
        ids: WorksiteSyncResult.ChangeIds,
    ) {
        db.withTransaction {
            val worksiteDao = db.worksiteDao()
            val networkId = ids.worksiteNetworkId
            if (networkId > 0) {
                worksiteDao.updateRootNetworkId(worksiteId, ids.worksiteNetworkId)
                worksiteDao.updateWorksiteNetworkId(worksiteId, ids.worksiteNetworkId)
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