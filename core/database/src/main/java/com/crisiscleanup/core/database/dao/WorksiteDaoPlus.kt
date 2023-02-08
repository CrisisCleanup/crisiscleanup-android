package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.WorksiteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorksiteDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    private suspend fun getWorksiteLocalModifiedAt(
        incidentId: Long,
        worksiteIds: Set<Long>,
        worksiteDao: WorksiteDao,
    ): Map<Long, Instant> {

        val worksitesUpdatedAt = worksiteDao.getWorksitesLocalModifiedAt(
            incidentId,
            worksiteIds,
        ).first()
        val modifiedAtLookup = mutableMapOf<Long, Instant>()
        worksitesUpdatedAt.forEach { modifiedAtLookup[it.networkId] = it.localModifiedAt }
        return modifiedAtLookup
    }

    /**
     * Saves external worksite data skipping worksites where local changes are newer than external changes
     */
    suspend fun syncExternalWorksites(
        incidentId: Long,
        worksites: List<WorksiteEntity>,
        syncedAt: Instant,
    ) {
        val worksiteIds = worksites.map(WorksiteEntity::networkId).toSet()
        db.withTransaction {
            val worksiteDao = db.worksiteDao()

            val modifiedAtLookup = getWorksiteLocalModifiedAt(incidentId, worksiteIds, worksiteDao)
            worksites.forEach { worksite ->
                val expectedLocalModifiedAt = modifiedAtLookup[worksite.networkId]
                if (expectedLocalModifiedAt == null) {
                    val id = worksiteDao.insertWorksiteRoot(
                        syncedAt,
                        worksite.networkId,
                        worksite.incidentId,
                    )
                    worksiteDao.insertWorksite(worksite.copy(id = id))

                } else if (worksite.updatedAt.epochSeconds > expectedLocalModifiedAt.epochSeconds) {
                    worksiteDao.updateSyncWorksiteRoot(
                        expectedLocalModifiedAt = expectedLocalModifiedAt,
                        syncedAt = syncedAt,
                        networkId = worksite.networkId,
                        incidentId = worksite.incidentId,
                    )
                    worksiteDao.updateSyncWorksite(
                        expectedLocalModifiedAt = expectedLocalModifiedAt,
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
                    // TODO All cross reference data. Be sure to delete XRs as necessary before updating new
                } else {
                    // TODO Log local modified not overwritten (for sync logs)
                }
            }
        }
    }

    fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeMin: Double,
        latitudeMax: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int = 600,
        offset: Int = 0,
    ): Flow<List<PopulatedWorksiteMapVisual>> {
        val worksiteDao = db.worksiteDao()
        val isLongitudeOrdered = longitudeLeft < longitudeRight
        return if (isLongitudeOrdered)
            worksiteDao.getWorksitesMapVisual(
                incidentId,
                latitudeMin,
                latitudeMax,
                longitudeLeft,
                longitudeRight,
                limit,
                offset,
            )
        else worksiteDao.getWorksitesMapVisualLongitudeCrossover(
            incidentId,
            latitudeMin,
            latitudeMax,
            longitudeLeft,
            longitudeRight,
            limit,
            offset,
        )
    }
}