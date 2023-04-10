package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import javax.inject.Inject

class WorksiteDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    private suspend fun getWorksiteLocalModifiedAt(
        incidentId: Long,
        worksiteIds: Set<Long>,
    ): Map<Long, WorksiteLocalModifiedAt> = db.withTransaction {
        val worksitesUpdatedAt = db.worksiteDao().getWorksitesLocalModifiedAt(
            incidentId,
            worksiteIds,
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
        val networkIds = flags.map(WorksiteFlagEntity::networkId)
        val flagDao = db.worksiteFlagDao()
        flagDao.syncDeleteUnspecified(worksiteId, networkIds)
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
            // TODO Log local modified not overwritten (for sync logs)
        }

        return@withTransaction false
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
        fun throwSizeMismatch(size: Int, dataName: String) {
            if (worksites.size != size) {
                throw Exception("Inconsistent data size. Each worksite must have corresponding $dataName.")
            }
        }
        throwSizeMismatch(worksitesWorkTypes.size, "work types")

        val worksiteIds = worksites.map(WorksiteEntity::networkId).toSet()
        db.withTransaction {
            val modifiedAtLookup = getWorksiteLocalModifiedAt(incidentId, worksiteIds)

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

    suspend fun syncWorksite(
        incidentId: Long,
        worksite: WorksiteEntity,
        workTypes: List<WorkTypeEntity>,
        formData: List<WorksiteFormDataEntity>,
        flags: List<WorksiteFlagEntity>,
        notes: List<WorksiteNoteEntity>,
        syncedAt: Instant,
    ): Long = db.withTransaction {
        val modifiedAtLookup = getWorksiteLocalModifiedAt(incidentId, setOf(worksite.networkId))
        val modifiedAt = modifiedAtLookup[worksite.networkId]
        val isUpdated = syncWorksite(
            incidentId,
            worksite,
            modifiedAt,
            workTypes,
            syncedAt,
            formData,
            flags,
            notes,
        )

        return@withTransaction if (isUpdated) db.worksiteDao()
            .getWorksiteId(incidentId, worksite.networkId) else -1
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
}