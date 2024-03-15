package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Worksites
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.data.util.IncidentDataPullStats
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.database.model.BoundedSyncedWorksiteIds
import com.crisiscleanup.core.database.model.CoordinateGridQuery
import com.crisiscleanup.core.database.model.IncidentWorksitesFullSyncStatsEntity
import com.crisiscleanup.core.database.model.PopulatedIncidentSyncStats
import com.crisiscleanup.core.database.model.SwNeBounds
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.time.Duration.Companion.days

data class IncidentProgressPullStats(
    val incidentName: String = "",
    val pullCount: Int = 0,
    val totalCount: Int = 0,
    val isApproximateTotal: Boolean = false,
)

interface WorksitesFullSyncer {
    val fullPullStats: Flow<IncidentProgressPullStats>
    val secondaryDataPullStats: Flow<IncidentDataPullStats>
    suspend fun sync(incidentId: Long)
}

// TODO Test coverage

@Singleton
class IncidentWorksitesFullSyncer @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val incidentDao: IncidentDao,
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksiteSyncStatDao: WorksiteSyncStatDao,
    private val deviceInspector: SyncCacheDeviceInspector,
    private val locationProvider: LocationProvider,
    private val secondaryDataSyncer: WorksitesSecondaryDataSyncer,
    @Logger(Worksites) private val logger: AppLogger,
) : WorksitesFullSyncer {
    override val fullPullStats = MutableStateFlow(IncidentProgressPullStats())
    override val secondaryDataPullStats = secondaryDataSyncer.dataPullStats

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
                saveWorksitesFull(
                    syncStats,
                    fullStats,
                )
            }
        }
    }

    // TODO Use connection strength to determine count
    private val fullSyncPageCount = 40
    private val idSyncPageCount = 20

    private suspend fun saveWorksitesFull(
        syncStats: PopulatedIncidentSyncStats,
        fullStats: IncidentWorksitesFullSyncStatsEntity,
    ) = coroutineScope {
        val incidentId = fullStats.incidentId

        val locationQueryParameters = fullStats.asQueryParameters(locationProvider)
        val pullAll = fullStats.syncedAt == null || locationQueryParameters.hasLocationChange
        val pageCountScale = if (deviceInspector.isLimitedDevice) 1 else 2
        val syncPageCount = fullSyncPageCount * pageCountScale
        val largeIncidentWorksitesCount = syncPageCount * 15

        val worksiteCount = networkDataSource.getWorksitesCount(incidentId)
        val syncStartedAt = Clock.System.now()

        ensureActive()

        val incident = incidentDao.getIncident(incidentId)!!.entity
        val incidentName = incident.shortName

        fun updateProgress(
            savedCount: Int,
            totalCount: Int = worksiteCount,
            isApproximate: Boolean = false,
        ) {
            fullPullStats.value = IncidentProgressPullStats(
                incidentName,
                savedCount,
                totalCount,
                isApproximate,
            )
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
                        worksiteDaoPlus.syncWorksites(entities, syncStartedAt)
                        pagedCount += worksites.size

                        updateProgress(pagedCount)

                        ensureActive()
                    } else {
                        break
                    }
                }
                if (pagedCount >= worksiteCount) {
                    worksiteSyncStatDao.upsert(
                        fullStats.copy(syncedAt = syncStartedAt),
                    )
                }
            } else {
                secondaryDataSyncer.sync(incidentId, syncStats)

                if (locationQueryParameters.hasLocation) {
                    val isSynced = saveWorksitesAroundLocation(
                        incidentId,
                        syncStartedAt,
                        locationQueryParameters,
                    ) { savedCount: Int, totalCount: Int ->
                        updateProgress(savedCount, totalCount, true)
                    }
                    if (isSynced) {
                        worksiteSyncStatDao.upsert(
                            fullStats.copy(
                                syncedAt = syncStartedAt,
                                latitude = locationQueryParameters.latitude,
                                longitude = locationQueryParameters.longitude,
                                radius = locationQueryParameters.searchRadius,
                            ),
                        )
                    }
                } else {
                    // TODO Signal user to set sync center and radius
                }
            }
        } else {
            // val newWorksitesCount = worksiteCount - initialSyncCount
            // TODO Delta pull IDs by date ranges and sync in batches. Cancelable.
        }
    }

    private suspend fun saveWorksitesAroundLocation(
        incidentId: Long,
        syncStartedAt: Instant,
        locationQueryParameters: LocationQueryParameters,
        updateProgress: (Int, Int) -> Unit,
    ) = coroutineScope {
        val (_, _, latitude, longitude, searchRadius) = locationQueryParameters
        val radialDegrees = searchRadius / 111.0
        val areaBounds = SwNeBounds(
            south = (latitude - radialDegrees).coerceAtLeast(-90.0),
            north = (latitude + radialDegrees).coerceAtMost(90.0),
            west = (longitude - radialDegrees).coerceAtLeast(-180.0),
            east = (longitude + radialDegrees).coerceAtMost(180.0),
        )

        // TODO Better structure and containment for everything below
        val boundedWorksiteRectCount = worksiteDao.getBoundedWorksiteCount(
            incidentId,
            latitudeSouth = areaBounds.south,
            latitudeNorth = areaBounds.north,
            longitudeWest = areaBounds.west,
            longitudeEast = areaBounds.east,
        )

        val gridQuery = CoordinateGridQuery(areaBounds)
        val byIdPageCount = idSyncPageCount
        gridQuery.initializeGrid(boundedWorksiteRectCount, byIdPageCount)

        fun updateIdSyncProgress(savedCount: Int, totalCount: Int = boundedWorksiteRectCount) {
            updateProgress(savedCount, totalCount)
        }

        var queryBounds = gridQuery.getSwNeGridCells()
        var boundedIds = mutableListOf<BoundedSyncedWorksiteIds>()
        val maxQueryCount = boundedWorksiteRectCount * 2 / byIdPageCount
        val now = Clock.System.now()
        val recentSyncDuration = 1.days
        var i = 0
        var queryCount = 0
        updateIdSyncProgress(0)
        var skipCount = 0
        while (i++ < maxQueryCount && queryBounds.isNotEmpty()) {
            queryBounds = worksiteDaoPlus.loadBoundedSyncedWorksiteIds(
                incidentId,
                boundedIds,
                byIdPageCount,
                queryBounds,
            ) {
                val syncWorksite = now - it.syncedAt > recentSyncDuration ||
                    it.formData.isEmpty()
                if (!syncWorksite) {
                    skipCount++
                }
                syncWorksite
            }

            val splitBoundedIds = boundedIds.size > byIdPageCount
            val networkQueryIds =
                if (splitBoundedIds) {
                    boundedIds.subList(0, byIdPageCount)
                } else {
                    boundedIds
                }
            boundedIds = if (boundedIds.size > byIdPageCount) {
                boundedIds.subList(byIdPageCount, boundedIds.size).toMutableList()
            } else {
                mutableListOf()
            }

            if (networkQueryIds.isEmpty()) {
                continue
            }

            val queryIds = networkQueryIds.map(BoundedSyncedWorksiteIds::networkId)
            val worksites = networkDataSource.getWorksites(queryIds)
            if (worksites?.isNotEmpty() == true) {
                val approximateTotalCount = boundedWorksiteRectCount - skipCount
                updateIdSyncProgress(
                    queryCount + (worksites.size * 0.5f).toInt(),
                    approximateTotalCount,
                )
                val entities = worksites.map { it.asEntities() }
                worksiteDaoPlus.syncWorksites(entities, syncStartedAt)
                queryCount += worksites.size

                updateIdSyncProgress(queryCount, approximateTotalCount)

                ensureActive()
            } else {
                break
            }
        }

        // TODO Be more precise
        boundedWorksiteRectCount == 0 || (queryCount > 0 && queryBounds.isEmpty())
    }
}

private data class LocationQueryParameters(
    val hasLocation: Boolean,
    val hasLocationChange: Boolean,
    val latitude: Double,
    val longitude: Double,
    val searchRadius: Double,
)

private suspend fun IncidentWorksitesFullSyncStatsEntity.asQueryParameters(
    locationProvider: LocationProvider,
): LocationQueryParameters {
    var latitude = this.latitude
    var longitude = this.longitude
    if (isMyLocationCentered) {
        locationProvider.getLocation()?.let { location ->
            latitude = location.first
            longitude = location.second
        }
    }

    var searchRadius = radius
    if (searchRadius < 5 && isMyLocationCentered) {
        searchRadius = 40.0
    }
    val locationChangeLength = searchRadius * 0.5
    val hasLocation = abs(latitude) <= 90 && abs(longitude) <= 180 && searchRadius > 0
    val hasLocationChange = hasLocation &&
        abs(latitude - this.latitude) * 111 > locationChangeLength &&
        abs(longitude - this.longitude) * 111 > locationChangeLength

    return LocationQueryParameters(
        hasLocation,
        hasLocationChange,
        latitude,
        longitude,
        searchRadius,
    )
}
