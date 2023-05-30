package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.database.model.IncidentWorksitesFullSyncStatsEntity
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class IncidentProgressPullStats(
    val incidentName: String = "",
    val pullCount: Int = 0,
    val totalCount: Int = 0,
)

interface WorksitesFullSyncer {
    val fullPullStats: Flow<IncidentProgressPullStats>

    suspend fun sync(incidentId: Long)
}

// TODO Test coverage

@Singleton
class IncidentWorksitesFullSyncer @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val incidentDao: IncidentDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksiteSyncStatDao: WorksiteSyncStatDao,
    memoryStats: AppMemoryStats,
    private val locationProvider: LocationProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : WorksitesFullSyncer {
    override val fullPullStats = MutableStateFlow(IncidentProgressPullStats())

    override suspend fun sync(incidentId: Long) {
        worksiteSyncStatDao.getIncidentSyncStats(incidentId)?.let { syncStats ->
            if (syncStats.isShortSynced()) {
                val fullStats = syncStats.fullStats ?: IncidentWorksitesFullSyncStatsEntity(
                    syncStats.entity.incidentId,
                    syncedAt = null,
                    isMyLocationCentered = false,
                    latitude = 999.0,
                    longitude = 999.0,
                    radius = 0.0,
                )
                saveWorksitesFull(syncStats.entity.targetCount, fullStats)
            }
        }
    }

    private val allWorksitesMemoryThreshold = 100
    private val isCapableDevice = memoryStats.availableMemory >= allWorksitesMemoryThreshold

    private val fullSyncPageCount = 40

    private suspend fun saveWorksitesFull(
        initialSyncCount: Int,
        syncStats: IncidentWorksitesFullSyncStatsEntity
    ) = coroutineScope {
        val incidentId = syncStats.incidentId

        var latitude = syncStats.latitude
        var longitude = syncStats.longitude
        if (syncStats.isMyLocationCentered) {
            locationProvider.getLocation()?.let { location ->
                latitude = location.first
                longitude = location.second
            }
        }

        val searchRadius = syncStats.radius
        val locationChangeLength = searchRadius * 0.1
        val hasLocation = abs(latitude) <= 90 && abs(longitude) <= 180 && searchRadius > 0
        val hasLocationChange = hasLocation &&
                abs(latitude - syncStats.latitude) * 111 > locationChangeLength &&
                abs(longitude - syncStats.longitude) * 111 > locationChangeLength

        var isSynced = false
        var pullAll = syncStats.syncedAt == null
        // TODO Adjust according to strength of network connection in real time
        val syncPageCount = fullSyncPageCount * (if (isCapableDevice) 2 else 1)
        val largeIncidentWorksitesCount = syncPageCount * 15

        val worksiteCount = networkDataSource.getWorksitesCount(incidentId)
        val syncStartedAt = Clock.System.now()

        ensureActive()

        val incident = incidentDao.getIncident(incidentId)!!.entity
        val incidentName = incident.shortName

        if (!pullAll) {
            val approximateDeltaCount = worksiteCount - initialSyncCount
            if (approximateDeltaCount < syncPageCount) {
                // TODO Delta pull IDs then all. Single pull.
            } else {
                if (hasLocationChange) {
                    pullAll = true
                } else {
                    // TODO Delta pull IDs by date ranges then all. Multi sync. Cancelable.
                }
            }
        }

        fun updateProgress(savedCount: Int) {
            fullPullStats.value = IncidentProgressPullStats(incidentName, savedCount, worksiteCount)
        }

        if (pullAll) {
            if (worksiteCount < largeIncidentWorksitesCount) {
                updateProgress(0)
                var pagedCount = 0
                while (pagedCount < worksiteCount) {
                    val worksites = networkDataSource.getWorksitesCoreData(
                        incidentId,
                        syncPageCount,
                        pagedCount,
                    )
                    if (worksites?.isNotEmpty() == true) {
                        updateProgress(pagedCount + (worksites.size * 0.5f).toInt())
                        val entities = worksites.map { it.asEntities() }
                        worksiteDaoPlus.syncWorksites(incidentId, entities, syncStartedAt)
                        pagedCount += worksites.size

                        updateProgress(pagedCount)

                        ensureActive()
                    } else {
                        break
                    }
                }
                isSynced = pagedCount >= worksiteCount
            } else {
                if (hasLocation) {
                    // TODO Query all worksite IDs in the location account for limit
                    //      Greater than limit should page by boundaries
                    //       Cancelable.

                } else {
                    // TODO Signal user to set worksite center and radius
                }
            }
        }

        if (isSynced) {
            worksiteSyncStatDao.upsert(syncStats.copy(syncedAt = syncStartedAt))
        }
    }
}