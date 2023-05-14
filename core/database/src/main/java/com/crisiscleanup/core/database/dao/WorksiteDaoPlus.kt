package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntities
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteLocalModifiedAt
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

class WorksiteDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
    private val syncLogger: SyncLogger,
) {
    private suspend fun getWorksiteLocalModifiedAt(
        incidentId: Long,
        networkWorksiteIds: Set<Long>,
    ): Map<Long, WorksiteLocalModifiedAt> = db.withTransaction {
        val worksitesUpdatedAt = db.worksiteDao().getWorksitesLocalModifiedAt(
            incidentId,
            networkWorksiteIds,
        )
        return@withTransaction worksitesUpdatedAt.associateBy { it.networkId }
    }

    /**
     * Syncs a worksite work types
     *
     * Deletes existing work types not specified and upserts work types specified.
     */
    private suspend fun syncWorkTypes(
        worksiteId: Long,
        unassociatedWorkTypes: List<WorkTypeEntity>,
    ) = db.withTransaction {
        if (unassociatedWorkTypes.isEmpty()) {
            return@withTransaction
        }

        val worksiteWorkTypes = unassociatedWorkTypes.map { it.copy(worksiteId = worksiteId) }
        val networkIds = worksiteWorkTypes.map(WorkTypeEntity::networkId)
        val workTypeDao = db.workTypeDao()
        workTypeDao.syncDeleteUnspecified(worksiteId, networkIds)
        val daoPlus = WorkTypeDaoPlus(db)
        daoPlus.syncUpsert(worksiteWorkTypes)
    }

    private suspend fun syncFormData(
        worksiteId: Long,
        unassociatedFormData: List<WorksiteFormDataEntity>,
    ) = db.withTransaction {
        if (unassociatedFormData.isEmpty()) {
            return@withTransaction
        }

        val worksiteFormData = unassociatedFormData.map { it.copy(worksiteId = worksiteId) }
        val fieldKeys = worksiteFormData.map(WorksiteFormDataEntity::fieldKey)
        val formDataDao = db.worksiteFormDataDao()
        formDataDao.deleteUnspecifiedKeys(worksiteId, fieldKeys)
        formDataDao.upsert(worksiteFormData)
    }

    private suspend fun syncFlags(
        worksiteId: Long,
        unassociatedFlags: List<WorksiteFlagEntity>,
    ) = db.withTransaction {
        if (unassociatedFlags.isEmpty()) {
            return@withTransaction
        }

        val flags = unassociatedFlags.map { it.copy(worksiteId = worksiteId) }
        val flagDao = db.worksiteFlagDao()
        val reasons = flags.map(WorksiteFlagEntity::reasonT)
        flagDao.syncDeleteUnspecified(worksiteId, reasons)
        val daoPlus = WorksiteFlagDaoPlus(db)
        daoPlus.syncUpsert(flags)
    }

    private suspend fun syncNotes(
        worksiteId: Long,
        unassociatedNotes: List<WorksiteNoteEntity>,
    ) = db.withTransaction {
        if (unassociatedNotes.isEmpty()) {
            return@withTransaction
        }

        val notes = unassociatedNotes.map { it.copy(worksiteId = worksiteId) }
        val networkIds = notes.map(WorksiteNoteEntity::networkId)
        val noteDao = db.worksiteNoteDao()
        noteDao.syncDeleteUnspecified(worksiteId, networkIds)
        val daoPlus = WorksiteNoteDaoPlus(db)
        daoPlus.syncUpsert(notes)
    }

    private suspend fun syncWorksite(
        incidentId: Long,
        worksite: WorksiteEntity,
        modifiedAt: WorksiteLocalModifiedAt?,
        workTypes: List<WorkTypeEntity>,
        syncedAt: Instant,
        formData: List<WorksiteFormDataEntity>? = null,
        flags: List<WorksiteFlagEntity>? = null,
        notes: List<WorksiteNoteEntity>? = null,
    ): Boolean = db.withTransaction {
        val worksiteDao = db.worksiteDao()

        val isLocallyModified = modifiedAt?.isLocallyModified ?: false
        if (modifiedAt == null) {
            val id = worksiteDao.insertOrRollbackWorksiteRoot(
                syncedAt,
                networkId = worksite.networkId,
                incidentId = worksite.incidentId,
            )
            worksiteDao.insert(worksite.copy(id = id))

            syncWorkTypes(id, workTypes)
            formData?.let { syncFormData(id, it) }
            flags?.let { syncFlags(id, it) }
            notes?.let { syncNotes(id, it) }

            return@withTransaction true

        } else if (!isLocallyModified) {
            worksiteDao.syncUpdateWorksiteRoot(
                id = modifiedAt.id,
                expectedLocalModifiedAt = modifiedAt.localModifiedAt,
                syncedAt = syncedAt,
                networkId = worksite.networkId,
                incidentId = worksite.incidentId,
            )
            worksiteDao.syncUpdateWorksite(
                id = modifiedAt.id,
                networkId = worksite.networkId,
                incidentId = worksite.incidentId,
                address = worksite.address,
                autoContactFrequencyT = worksite.autoContactFrequencyT,
                caseNumber = worksite.caseNumber,
                city = worksite.city,
                county = worksite.county,
                createdAt = worksite.createdAt,
                email = worksite.email,
                favoriteId = worksite.favoriteId,
                keyWorkTypeType = worksite.keyWorkTypeType,
                keyWorkTypeOrgClaim = worksite.keyWorkTypeOrgClaim,
                keyWorkTypeStatus = worksite.keyWorkTypeStatus,
                latitude = worksite.latitude,
                longitude = worksite.longitude,
                name = worksite.name,
                phone1 = worksite.phone1,
                phone2 = worksite.phone2,
                plusCode = worksite.plusCode,
                postalCode = worksite.postalCode,
                reportedBy = worksite.reportedBy,
                state = worksite.state,
                svi = worksite.svi,
                what3Words = worksite.what3Words,
                updatedAt = worksite.updatedAt,
            )

            // Should return a valid ID if UPDATE OR ROLLBACK query succeeded
            val worksiteId = worksiteDao.getWorksiteId(incidentId, worksite.networkId)

            syncWorkTypes(worksiteId, workTypes)
            formData?.let { syncFormData(worksiteId, it) }
            flags?.let { syncFlags(worksiteId, it) }
            notes?.let { syncNotes(worksiteId, it) }

            return@withTransaction true

        } else {
            // Resolving changes at this point is not worth the complexity.
            // Defer to worksite (snapshot) changes resolving successfully and completely.
            syncLogger.log("Skip sync overwriting locally modified worksite ${worksite.id} (${worksite.networkId})")
        }

        return@withTransaction false
    }

    private fun throwSizeMismatch(worksitesSize: Int, size: Int, dataName: String) {
        if (worksitesSize != size) {
            throw Exception("Inconsistent data size. Each worksite must have corresponding $dataName.")
        }
    }

    /**
     * Syncs worksite data skipping worksites where local changes exist
     *
     * @return Number of worksites inserted/updated
     */
    suspend fun syncWorksites(
        incidentId: Long,
        worksites: List<WorksiteEntity>,
        worksitesWorkTypes: List<List<WorkTypeEntity>>,
        syncedAt: Instant,
    ) {
        throwSizeMismatch(worksites.size, worksitesWorkTypes.size, "work types")

        val networkWorksiteIds = worksites.map(WorksiteEntity::networkId).toSet()
        db.withTransaction {
            val modifiedAtLookup = getWorksiteLocalModifiedAt(incidentId, networkWorksiteIds)

            worksites.forEachIndexed { i, worksite ->
                val workTypes = worksitesWorkTypes[i]
                val modifiedAt = modifiedAtLookup[worksite.networkId]
                syncWorksite(
                    incidentId,
                    worksite,
                    modifiedAt,
                    workTypes,
                    syncedAt,
                )
            }
        }
    }

    // TODO Write tests
    suspend fun syncShortFlags(
        incidentId: Long,
        worksites: List<WorksiteEntity>,
        worksitesFlags: List<List<WorksiteFlagEntity>>,
    ) {
        throwSizeMismatch(worksites.size, worksitesFlags.size, "flags")

        val networkWorksiteIds = worksites.map(WorksiteEntity::networkId).toSet()
        db.withTransaction {
            val modifiedAtLookup = getWorksiteLocalModifiedAt(incidentId, networkWorksiteIds)

            val worksiteDao = db.worksiteDao()
            val flagDao = db.worksiteFlagDao()
            worksitesFlags.forEachIndexed { i, flags ->
                val networkWorksiteId = worksites[i].networkId
                val modifiedAt = modifiedAtLookup[networkWorksiteId]
                val isLocallyModified = modifiedAt?.isLocallyModified ?: false
                if (!isLocallyModified) {
                    val worksiteId = worksiteDao.getWorksiteId(incidentId, networkWorksiteId)
                    val flagReasons = flags.map(WorksiteFlagEntity::reasonT)
                    flagDao.syncDeleteUnspecified(worksiteId, flagReasons)
                    val updatedFlags = flags.map { it.copy(worksiteId = worksiteId) }
                    flagDao.insertIgnore(updatedFlags)
                }
            }
        }
    }

    suspend fun syncWorksite(
        incidentId: Long,
        entities: WorksiteEntities,
        syncedAt: Instant,
    ): Pair<Boolean, Long> = db.withTransaction {
        val core = entities.core
        val modifiedAtLookup = getWorksiteLocalModifiedAt(incidentId, setOf(core.networkId))
        val modifiedAt = modifiedAtLookup[core.networkId]
        val isUpdated = syncWorksite(
            incidentId,
            core,
            modifiedAt,
            entities.workTypes,
            syncedAt,
            entities.formData,
            entities.flags,
            entities.notes,
        )

        val worksiteId =
            if (isUpdated) db.worksiteDao().getWorksiteId(incidentId, core.networkId)
            else -1
        return@withTransaction Pair(isUpdated, worksiteId)
    }

    suspend fun syncFillWorksite(
        incidentId: Long,
        entities: WorksiteEntities,
    ): Boolean = db.withTransaction {
        val worksiteDao = db.worksiteDao()
        val (core, flags, formData, notes, workTypes) = entities
        val worksiteId = worksiteDao.getWorksiteId(incidentId, core.networkId)
        if (worksiteId > 0) {
            with(core) {
                worksiteDao.syncFillWorksite(
                    worksiteId,
                    autoContactFrequencyT,
                    caseNumber,
                    email,
                    favoriteId,
                    phone1,
                    phone2,
                    plusCode,
                    svi,
                    reportedBy,
                    what3Words,
                )
            }

            val flagDao = db.worksiteFlagDao()
            val flagsReasons = flagDao.getReasons(worksiteId).toSet()
            val newFlags = flags.filter { !flagsReasons.contains(it.reasonT) }
                .map { it.copy(worksiteId = worksiteId) }
            flagDao.insertIgnore(newFlags)

            val formDataDao = db.worksiteFormDataDao()
            val formDataKeys = formDataDao.getDataKeys(worksiteId).toSet()
            val newFormData = formData.filter { !formDataKeys.contains(it.fieldKey) }
                .map { it.copy(worksiteId = worksiteId) }
            formDataDao.upsert(newFormData)

            val noteDao = db.worksiteNoteDao()
            val recentNotes = noteDao.getNotes(worksiteId).map(String::trim).toSet()
            val newNotes = notes.filter { !recentNotes.contains(it.note) }
                .map { it.copy(worksiteId = worksiteId) }
            noteDao.insertIgnore(newNotes)

            val workTypeDao = db.workTypeDao()
            val workTypeKeys = workTypeDao.getWorkTypes(worksiteId).toSet()
            val newWorkTypes = workTypes.filter { !workTypeKeys.contains(it.workType) }
                .map { it.copy(worksiteId = worksiteId) }
            workTypeDao.insertIgnore(newWorkTypes)

            return@withTransaction true
        }
        return@withTransaction false
    }

    fun streamWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int = 600,
        offset: Int = 0,
    ): Flow<List<PopulatedWorksiteMapVisual>> {
        val worksiteDao = db.worksiteDao()
        val isLongitudeOrdered = longitudeLeft < longitudeRight
        return if (isLongitudeOrdered)
            worksiteDao.streamWorksitesMapVisual(
                incidentId,
                latitudeSouth,
                latitudeNorth,
                longitudeLeft,
                longitudeRight,
                limit,
                offset,
            )
        else worksiteDao.streamWorksitesMapVisualLongitudeCrossover(
            incidentId,
            latitudeSouth,
            latitudeNorth,
            longitudeLeft,
            longitudeRight,
            limit,
            offset,
        )
    }

    fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
    ): Int {
        val worksiteDao = db.worksiteDao()
        val isLongitudeOrdered = longitudeLeft < longitudeRight
        return if (isLongitudeOrdered)
            worksiteDao.getWorksitesCount(
                incidentId,
                latitudeSouth,
                latitudeNorth,
                longitudeLeft,
                longitudeRight,
            )
        else worksiteDao.getWorksitesCountLongitudeCrossover(
            incidentId,
            latitudeSouth,
            latitudeNorth,
            longitudeLeft,
            longitudeRight,
        )
    }

    suspend fun onSyncEnd(worksiteId: Long, syncedAt: Instant = Clock.System.now()): Boolean {
        return db.withTransaction {
            val hasModification = db.worksiteFlagDao().getUnsyncedCount(worksiteId) > 0 ||
                    db.worksiteNoteDao().getUnsyncedCount(worksiteId) > 0 ||
                    db.workTypeDao().getUnsyncedCount(worksiteId) > 0 ||
                    db.worksiteChangeDao().getChangeCount(worksiteId) > 0
            return@withTransaction if (hasModification) {
                false
            } else {
                db.worksiteDao().setRootUnmodified(worksiteId, syncedAt)
                db.workTypeTransferRequestDao().deleteUnsynced(worksiteId)
                true
            }
        }
    }

    fun getUnsyncedChangeCount(worksiteId: Long): List<Int> = listOf(
        db.worksiteFlagDao().getUnsyncedCount(worksiteId),
        db.worksiteNoteDao().getUnsyncedCount(worksiteId),
        db.workTypeDao().getUnsyncedCount(worksiteId),
        db.worksiteChangeDao().getChangeCount(worksiteId),
    )
}