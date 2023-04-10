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
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.database.model.asEntities
import com.crisiscleanup.core.model.data.*
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

        val localFlagIds = worksite.flags?.map(WorksiteFlag::id) ?: emptyList()
        val flagIdMap = db.worksiteFlagDao()
            .getNetworkedIdMap(
                worksite.id,
                localFlagIds.toSet(),
            )
            .associate { it.id to it.networkId }

        val localNoteIds = worksite.notes.map(WorksiteNote::id).toSet()
        val noteIdMap = db.worksiteNoteDao()
            .getNetworkedIdMap(
                worksite.id,
                localNoteIds,
            )
            .associate { it.id to it.networkId }

        val localWorkTypeIds = worksite.workTypes.map(WorkType::id).toSet()
        val workTypeIdMap = db.workTypeDao()
            .getNetworkedIdMap(
                worksite.id,
                localWorkTypeIds,
            )
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
                        localModifiedAt = changeEntities.core.updatedAt,
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
                        core.updatedAt,
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
                        flagDao.insert(inserts)
                        flagDao.update(updates)

                        syncLogger.log("Flags. Inserted ${inserts.size}. Updated ${updates.size}")
                    }

                formDataDao.upsert(formData)
                syncLogger.log("Form data. Upserted ${formData.size}.")

                noteDao.insert(insertNotes)
                syncLogger.log("Notes. Inserted ${insertNotes.size}.")

                workTypes.split { it.id <= 0 }.also { (inserts, updates) ->
                    workTypeDao.insert(inserts)
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
        val serializedChange = changeSerializer.serialize(
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
            serializedChange,
        )
        db.worksiteChangeDao().insertChange(changeEntity)
    }
}

private data class IdNetworkIdMaps(
    val flag: Map<Long, Long> = emptyMap(),
    val note: Map<Long, Long> = emptyMap(),
    val workType: Map<Long, Long> = emptyMap(),
)