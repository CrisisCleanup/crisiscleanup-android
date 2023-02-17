package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteLocalModifiedAt
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorksiteDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    private fun getWorksiteLocalModifiedAt(
        incidentId: Long,
        worksiteIds: Set<Long>,
        worksiteDao: WorksiteDao,
    ): Map<Long, WorksiteLocalModifiedAt> {
        val worksitesUpdatedAt = worksiteDao.getWorksitesLocalModifiedAt(
            incidentId,
            worksiteIds,
        )
        return worksitesUpdatedAt.fold(mutableMapOf()) { map, w ->
            map[w.networkId] = w
            map
        }
    }

    /**
     * Syncs a worksite work types
     *
     * Deletes existing work types not specified and upserts work types specified.
     */
    private suspend fun syncWorkTypes(
        worksiteId: Long,
        unassociatedWorkTypes: List<WorkTypeEntity>,
    ) {
        if (unassociatedWorkTypes.isEmpty()) {
            return
        }

        val worksiteWorkTypes = unassociatedWorkTypes.map { it.copy(worksiteId = worksiteId) }
        val networkIds = worksiteWorkTypes.map(WorkTypeEntity::networkId)
        val workTypeDao = db.workTypeDao()
        workTypeDao.syncDeleteUnspecifiedWorkTypes(worksiteId, networkIds)

        val workTypeDaoPlus = WorkTypeDaoPlus(db)
        workTypeDaoPlus.syncUpsert(worksiteWorkTypes)
    }

    /**
     * Syncs worksite data skipping worksites where local changes exist
     */
    suspend fun syncWorksites(
        incidentId: Long,
        worksites: List<WorksiteEntity>,
        worksitesWorkTypes: List<List<WorkTypeEntity>>,
        syncedAt: Instant,
    ) {
        if (worksites.size != worksitesWorkTypes.size) {
            throw Exception("Inconsistent data size. Each worksite must have corresponding work types")
        }

        val worksiteIds = worksites.map(WorksiteEntity::networkId).toSet()
        db.withTransaction {
            val worksiteDao = db.worksiteDao()

            val modifiedAtLookup = getWorksiteLocalModifiedAt(incidentId, worksiteIds, worksiteDao)
            worksites.forEachIndexed { i, worksite ->
                val workTypes = worksitesWorkTypes[i]

                val modifiedAt = modifiedAtLookup[worksite.networkId]
                val isLocallyModified = modifiedAt?.isLocallyModified ?: false
                if (modifiedAt == null) {
                    val id = worksiteDao.insertOrRollbackWorksiteRoot(
                        syncedAt,
                        worksite.networkId,
                        worksite.incidentId,
                    )
                    worksiteDao.insertWorksite(worksite.copy(id = id))

                    syncWorkTypes(id, workTypes)

                    // TODO Sync more related data.

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

                    // TODO Sync more related data.. Be sure to delete existing as necessary before updating with new.

                } else {
                    // TODO Log local modified not overwritten (for sync logs)
                }
            }
        }
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
}