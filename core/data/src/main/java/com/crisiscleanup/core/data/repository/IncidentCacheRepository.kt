package com.crisiscleanup.core.data.repository

import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.IncidentMapTracker
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.common.kmToMiles
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.incidentcache.CountTimeTracker
import com.crisiscleanup.core.data.incidentcache.DataDownloadSpeedMonitor
import com.crisiscleanup.core.data.incidentcache.IncidentDataPullReporter
import com.crisiscleanup.core.data.incidentcache.IncidentDataPullStatsUpdater
import com.crisiscleanup.core.data.incidentcache.SyncCacheDeviceInspector
import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.data.model.IncidentDataSyncParameters
import com.crisiscleanup.core.data.model.IncidentPullDataType
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.asExternalModel
import com.crisiscleanup.core.data.model.asWorksiteEntity
import com.crisiscleanup.core.database.dao.IncidentDataSyncParameterDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.datastore.IncidentCachePreferencesDataSource
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentLocationBounder
import com.crisiscleanup.core.model.data.IncidentWorksitesCachePreferences
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.KeyDynamicValuePair
import com.crisiscleanup.core.network.model.NetworkFlagsFormData
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksitePage
import com.crisiscleanup.core.network.model.WorksiteDataResult
import com.crisiscleanup.core.network.model.WorksiteDataSubset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface IncidentCacheRepository {
    val isSyncingActiveIncident: Flow<Boolean>
    val cacheStage: Flow<IncidentCacheStage>

    val cachePreferences: Flow<IncidentWorksitesCachePreferences>

    fun streamSyncStats(incidentId: Long): Flow<IncidentDataSyncParameters?>

    /**
     * @return TRUE when plan is accepted or FALSE otherwise (already queued or unable to cancel ongoing)
     */
    suspend fun submitPlan(
        overwriteExisting: Boolean,
        forcePullIncidents: Boolean,
        cacheSelectedIncident: Boolean,
        cacheActiveIncidentWorksites: Boolean,
        cacheWorksitesAdditional: Boolean,
        restartCacheCheckpoint: Boolean,
        planTimeout: Duration = 9.seconds,
    ): Boolean

    suspend fun sync(): SyncResult

    suspend fun resetIncidentSyncStats(incidentId: Long)

    suspend fun updateCachePreferences(preferences: IncidentWorksitesCachePreferences)
}

@Singleton
class IncidentWorksitesCacheRepository @Inject constructor(
    private val accountDataRefresher: AccountDataRefresher,
    private val incidentsRepository: IncidentsRepository,
    private val appPreferences: LocalAppPreferencesDataSource,
    private val syncParameterDao: IncidentDataSyncParameterDao,
    private val incidentCachePreferences: IncidentCachePreferencesDataSource,
    incidentSelector: IncidentSelector,
    private val locationProvider: LocationProvider,
    private val locationBounder: IncidentLocationBounder,
    private val incidentMapTracker: IncidentMapTracker,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val worksitesRepository: WorksitesRepository,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val deviceInspector: SyncCacheDeviceInspector,
    private val speedMonitor: DataDownloadSpeedMonitor,
    private val connectivityManager: ConnectivityManager,
    private val syncLogger: SyncLogger,
    private val appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Sync) private val appLogger: AppLogger,
) : IncidentCacheRepository, IncidentDataPullReporter {
    private val isNetworkUnmetered: Boolean
        get() = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NET_CAPABILITY_NOT_METERED) == true

    override val incidentDataPullStats = MutableStateFlow(IncidentDataPullStats())
    override val onIncidentDataPullComplete = MutableStateFlow(EmptyIncident.id)

    private val syncPlanReference = AtomicReference(EmptySyncPlan)

    private val syncingIncidentId = MutableStateFlow(EmptyIncident.id)
    override val isSyncingActiveIncident = combine(
        incidentSelector.incidentId,
        syncingIncidentId,
        ::Pair,
    )
        .map { (incidentId, syncingId) ->
            incidentId == syncingId
        }

    override val cacheStage = MutableStateFlow(IncidentCacheStage.Start)

    override val cachePreferences = incidentCachePreferences.preferences

    override fun streamSyncStats(incidentId: Long) =
        syncParameterDao.streamWorksitesSyncStats(incidentId)
            .map { it?.asExternalModel(appLogger) }

    // TODO Write tests
    override suspend fun submitPlan(
        overwriteExisting: Boolean,
        forcePullIncidents: Boolean,
        cacheSelectedIncident: Boolean,
        cacheActiveIncidentWorksites: Boolean,
        cacheWorksitesAdditional: Boolean,
        restartCacheCheckpoint: Boolean,
        planTimeout: Duration,
    ): Boolean {
        val incidentIds = incidentsRepository.incidents.first()
            .map(Incident::id)
            .toSet()
        val selectedIncident = appPreferences.userData.first().selectedIncidentId
        val isIncidentCached = incidentIds.contains(selectedIncident)

        if (incidentIds.isNotEmpty() &&
            !isIncidentCached &&
            selectedIncident == EmptyIncident.id &&
            !forcePullIncidents
        ) {
            return false
        }

        val submittedPlan = IncidentDataSyncPlan(
            selectedIncident,
            syncIncidents = forcePullIncidents || !isIncidentCached,
            syncSelectedIncident = cacheSelectedIncident || !isIncidentCached,
            syncActiveIncidentWorksites = cacheActiveIncidentWorksites,
            syncWorksitesAdditional = cacheWorksitesAdditional,
            restartCache = restartCacheCheckpoint,
        )
        synchronized(syncPlanReference) {
            if (!overwriteExisting &&
                !submittedPlan.syncIncidents &&
                !restartCacheCheckpoint
            ) {
                with(syncPlanReference.get()) {
                    if (selectedIncident == incidentId &&
                        submittedPlan.timestamp - timestamp < planTimeout &&
                        submittedPlan.syncSelectedIncidentLevel <= syncSelectedIncidentLevel &&
                        submittedPlan.syncWorksitesLevel <= syncWorksitesLevel
                    ) {
                        syncLogger.log("Skipping redundant sync plan for $selectedIncident")
                        return false
                    }
                }
            }

            syncLogger.log("Setting sync plan for $selectedIncident")
            syncPlanReference.set(submittedPlan)
            return true
        }
    }

    private fun logStage(
        incidentId: Long,
        stage: IncidentCacheStage,
        details: String = "",
    ) {
        cacheStage.value = stage

        if (appEnv.isProduction) {
            return
        }

        val indentation = when (stage) {
            IncidentCacheStage.Start -> ""
            else -> "  "
        }
        val message = "$indentation$incidentId $stage $details".trimEnd()
        syncLogger.log(message)

        if (appEnv.isDebuggable) {
            appLogger.logDebug(message)
        }
    }

    override suspend fun sync() = coroutineScope {
        val syncPlan = syncPlanReference.get()

        val incidentId = syncPlan.incidentId
        syncingIncidentId.value = incidentId

        logStage(incidentId, IncidentCacheStage.Start)

        val partialSyncReasons = mutableListOf<String>()

        try {
            if (syncPlan.syncIncidents) {
                logStage(incidentId, IncidentCacheStage.Incidents)

                accountDataRefresher.updateApprovedIncidents(true)
                incidentsRepository.pullIncidents(true)
            }

            val syncPreferences = cachePreferences.first()

            val isPaused = syncPreferences.isPaused

            val incidents = incidentsRepository.incidents.first()
            if (incidents.isEmpty()) {
                return@coroutineScope SyncResult.Error("Failed to sync Incidents")
            }
            val incidentIds = incidents.map(Incident::id).toSet()
            if (!incidentIds.contains(incidentId)) {
                return@coroutineScope SyncResult.Partial("Incident not found. Waiting for Incident select.")
            }

            val incidentName = incidents.first { it.id == incidentId }.name
            val worksitesCoreStatsUpdater = IncidentDataPullStatsUpdater {
                incidentDataPullStats.value = it
            }.apply {
                beginPull(
                    incidentId,
                    incidentName,
                    IncidentPullDataType.WorksitesCore,
                )
            }

            if (syncPlan.syncSelectedIncident) {
                logStage(incidentId, IncidentCacheStage.ActiveIncident)

                incidentsRepository.pullIncident(incidentId)
            }

            if (syncPlan.restartCache) {
                ensureActive()

                logStage(
                    incidentId,
                    IncidentCacheStage.WorksitesCore,
                    "Restarting Worksites cache",
                )

                resetIncidentSyncStats(incidentId)
            }

            val syncStatsEntity = syncParameterDao.getSyncStats(incidentId)
            val regionParameters = syncPreferences.boundedRegionParameters
            val preferencesBoundedRegion = IncidentDataSyncParameters.BoundedRegion(
                latitude = regionParameters.regionLatitude,
                longitude = regionParameters.regionLongitude,
                radius = regionParameters.regionRadiusMiles,
            )
            val syncStats =
                syncStatsEntity?.asExternalModel(appLogger) ?: IncidentDataSyncParameters(
                    incidentId,
                    syncDataMeasures = IncidentDataSyncParameters.SyncDataMeasure.relative(),
                    preferencesBoundedRegion,
                    Instant.fromEpochSeconds(0),
                )
            if (syncStatsEntity == null) {
                syncParameterDao.insertSyncStats(syncStats.asEntity(appLogger))
            }

            ensureActive()

            var isSlowDownload = false
            var skipWorksiteCaching = false
            if (syncPreferences.isRegionBounded) {
                if (preferencesBoundedRegion.isDefined) {
                    cacheBoundedWorksites(
                        incidentId,
                        isPaused = isPaused,
                        isMyLocationBounded = regionParameters.isRegionMyLocation,
                        preferencesBoundedRegion = preferencesBoundedRegion,
                        savedBoundedRegion = syncStats.boundedRegion,
                        syncStats.boundedSyncedAt,
                        worksitesCoreStatsUpdater,
                    )
                } else {
                    partialSyncReasons.add("Incomplete bounded region. Skipping Worksites sync.")
                }
            } else {
                if (!isPaused) {
                    preloadBounded(incidentId, worksitesCoreStatsUpdater)
                }

                worksitesCoreStatsUpdater.setDeterminate()

                // TODO If not preloaded and times out try caching around coordinates
                val shortResult = cacheWorksitesCore(
                    incidentId,
                    isPaused,
                    syncStats,
                    worksitesCoreStatsUpdater,
                )
                isSlowDownload = shortResult.isSlow == true

                if (shortResult.isSlow == false &&
                    isPaused &&
                    !syncStats.syncDataMeasures.core.isDeltaSync
                ) {
                    cacheWorksitesCore(
                        incidentId,
                        false,
                        syncStats,
                        worksitesCoreStatsUpdater,
                    )
                }
            }

            ensureActive()
            worksitesCoreStatsUpdater.endPull()

            if (isPaused && isSlowDownload) {
                partialSyncReasons.add("Worksite downloads are paused")
                skipWorksiteCaching = true
            }

            if (syncPlan.syncSelectedIncident) {
                ensureActive()

                logStage(incidentId, IncidentCacheStage.ActiveIncidentOrganization)

                val organizationsStatsUpdater = IncidentDataPullStatsUpdater {
                    incidentDataPullStats.value = it
                }.apply {
                    beginPull(
                        incidentId,
                        incidentName,
                        IncidentPullDataType.Organizations,
                    )
                    setIndeterminate()
                }

                incidentsRepository.pullIncidentOrganizations(incidentId)

                ensureActive()
                organizationsStatsUpdater.endPull()
            }

            if (!(skipWorksiteCaching || syncPreferences.isRegionBounded)) {
                ensureActive()

                logStage(incidentId, IncidentCacheStage.WorksitesAdditional)

                val worksitesAdditionalStatsUpdater = IncidentDataPullStatsUpdater {
                    incidentDataPullStats.value = it
                }.apply {
                    beginPull(
                        incidentId,
                        incidentName,
                        IncidentPullDataType.WorksitesAdditional,
                    )
                }

                val additionalResult = cacheAdditionalWorksiteData(
                    incidentId,
                    isPaused,
                    syncStats,
                    worksitesAdditionalStatsUpdater,
                )

                if (additionalResult.isSlow == false &&
                    isPaused &&
                    !syncStats.syncDataMeasures.additional.isDeltaSync
                ) {
                    cacheAdditionalWorksiteData(
                        incidentId,
                        false,
                        syncStats,
                        worksitesCoreStatsUpdater,
                    )
                }

                ensureActive()
                worksitesAdditionalStatsUpdater.endPull()
            }
        } catch (e: Exception) {
            with(incidentDataPullStats.value) {
                if (queryCount < dataCount) {
                    speedMonitor.onSpeedChange(true)
                }
            }
            throw e
        } finally {
            if (syncPlanReference.compareAndSet(syncPlan, EmptySyncPlan)) {
                incidentDataPullStats.value = IncidentDataPullStats(isEnded = true)
                cacheStage.value = IncidentCacheStage.End
            }

            if (syncingIncidentId.compareAndSet(incidentId, EmptyIncident.id)) {
                logStage(incidentId, IncidentCacheStage.End)
            }

            syncLogger.flush()
        }

        return@coroutineScope if (partialSyncReasons.isEmpty()) {
            SyncResult.Success("Cached Incident $incidentId data.")
        } else {
            SyncResult.Partial(partialSyncReasons.joinToString("\n"))
        }
    }

    // TODO Refactor and write tests, remove exception guard
    /**
     * @return latitude, longitude
     */
    private suspend fun getLocation(incidentId: Long): Pair<Double, Double>? {
        locationProvider.getLocation(10.seconds)?.let {
            return it
        }

        try {
            val recentWorksites = worksitesRepository.getRecentWorksites(incidentId, limit = 3)
            if (recentWorksites.isNotEmpty()) {
                var totalLatitude = 0.0
                var totalLongitude = 0.0
                recentWorksites.forEach {
                    totalLatitude += it.latitude
                    totalLongitude += it.longitude
                }
                val averageLatitude = totalLatitude / recentWorksites.size
                val averageLongitude = totalLongitude / recentWorksites.size
                if (locationBounder.isInBounds(
                        incidentId,
                        latitude = averageLatitude,
                        longitude = averageLongitude,
                    )
                ) {
                    return Pair(averageLatitude, averageLongitude)
                }
            }

            incidentMapTracker.lastLocation?.let {
                if (locationBounder.isInBounds(
                        incidentId,
                        latitude = it.first,
                        longitude = it.second,
                    )
                ) {
                    return it
                }
            }

            locationBounder.getBoundsCenter(incidentId)?.let {
                if (locationBounder.isInBounds(
                        incidentId,
                        latitude = it.first,
                        longitude = it.second,
                    )
                ) {
                    return it
                }
            }
        } catch (e: Exception) {
            appLogger.logException(e)
        }

        return null
    }

    private suspend fun cacheBoundedWorksites(
        incidentId: Long,
        isPaused: Boolean,
        isMyLocationBounded: Boolean,
        preferencesBoundedRegion: IncidentDataSyncParameters.BoundedRegion,
        savedBoundedRegion: IncidentDataSyncParameters.BoundedRegion?,
        syncedAt: Instant,
        statsUpdater: IncidentDataPullStatsUpdater,
        boundsCacheTimeout: Duration = 1.minutes,
    ) = coroutineScope {
        val stage = IncidentCacheStage.WorksitesBounded

        fun log(message: String) = logStage(incidentId, stage, message)

        var boundedRegion = preferencesBoundedRegion

        if (isMyLocationBounded) {
            val locationCoordinates = getLocation(incidentId)
            locationCoordinates?.let {
                boundedRegion = boundedRegion.copy(
                    latitude = it.first,
                    longitude = it.second,
                )
            }
            if (locationCoordinates == null) {
                log(
                    "Current user location is not cached. Falling back to last set location.",
                )
            }
        }

        if (!boundedRegion.isDefined) {
            log("Bounding region (lat=${boundedRegion.latitude}, lng=${boundedRegion.longitude}, ${boundedRegion.radius} miles) not fully specified")
        } else if (Clock.System.now() - syncedAt < boundsCacheTimeout &&
            savedBoundedRegion?.isSignificantChange(boundedRegion) == false
        ) {
            log(
                "Skipping caching of bounded Worksites. Insignificant bounds change between saved $savedBoundedRegion and query $boundedRegion.",
            )
        } else {
            log("Caching bounded Worksites")

            val queryAfter = if (savedBoundedRegion?.isSignificantChange(boundedRegion) == true) {
                null
            } else {
                syncedAt
            }
            val countSpeed = cacheBounded(
                incidentId,
                isPaused,
                boundedRegion,
                statsUpdater,
                queryAfter,
                ::log,
            )
            if (!isPaused && countSpeed.count == 0) {
                // TODO Alert no Cases were found in the specified region
            }
        }
    }

    private suspend fun preloadBounded(
        incidentId: Long,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) {
        val localCount = worksitesRepository.getWorksitesCount(incidentId)
        if (localCount > 600) {
            return
        }

        val networkCount = networkDataSource.getWorksitesCount(incidentId)
        if (networkCount < 3000) {
            return
        }

        getLocation(incidentId)?.let {
            val boundedRegion = IncidentDataSyncParameters.BoundedRegion(
                latitude = it.first,
                longitude = it.second,
                radius = 15.0,
            )
            if (boundedRegion.isDefined) {
                fun log(message: String) {
                    logStage(
                        incidentId,
                        IncidentCacheStage.WorksitesPreload,
                        message,
                    )
                }

                try {
                    cacheBounded(
                        incidentId,
                        false,
                        boundedRegion,
                        statsUpdater,
                        log = ::log,
                        maxCount = 300,
                    )
                } catch (e: Exception) {
                    appLogger.logException(e)
                }
            }
        }
    }

    private suspend fun cacheBounded(
        incidentId: Long,
        isPaused: Boolean,
        boundedRegion: IncidentDataSyncParameters.BoundedRegion,
        statsUpdater: IncidentDataPullStatsUpdater,
        queryAfter: Instant? = null,
        log: (String) -> Unit,
        maxCount: Int = 5000,
    ) = coroutineScope {
        statsUpdater.setIndeterminate()

        val downloadSpeedTracker = CountTimeTracker()

        val queryCount = if (isPaused) {
            10
        } else if (appEnv.isProduction) {
            60
        } else {
            40
        }
        var queryPage = 1
        var savedWorksiteIds = emptySet<Long>()
        var initialCount = -1
        var savedCount = 0
        var isOuterRegionReached = false
        val syncStart = Clock.System.now()

        var liveRegion = boundedRegion
        var liveQueryAfter = queryAfter

        var isSlowDownload: Boolean? = null

        do {
            ensureActive()

            val locationDetails = "${liveRegion.latitude},${liveRegion.longitude}"

            val networkData = downloadSpeedTracker.time {
                val result = networkDataSource.getWorksitesPage(
                    incidentId,
                    pageCount = queryCount,
                    pageOffset = queryPage,
                    latitude = liveRegion.latitude,
                    longitude = liveRegion.longitude,
                    updatedAtAfter = liveQueryAfter,
                )
                if (initialCount < 0) {
                    initialCount = result.count ?: 0
                    statsUpdater.setDataCount(initialCount)
                }
                result.data ?: emptyList()
            }

            if (networkData.isEmpty()) {
                isOuterRegionReached = true

                log("Cached ($savedCount/$initialCount) Worksites around $locationDetails.")

                if (savedCount == 0) {
                    break
                }
            } else {
                isSlowDownload = downloadSpeedTracker.averageSpeed() < slowSpeedDownload
                speedMonitor.onSpeedChange(isSlowDownload)

                statsUpdater.addQueryCount(networkData.size)

                queryPage += 1

                val deduplicateWorksites = networkData.filter {
                    !savedWorksiteIds.contains(it.id)
                }
                if (deduplicateWorksites.isEmpty()) {
                    val duplicateCount = networkData.size - deduplicateWorksites.size
                    log("$duplicateCount duplicate(s), before")
                    break
                }

                saveWorksites(
                    deduplicateWorksites,
                    statsUpdater,
                )
                savedCount += deduplicateWorksites.size

                savedWorksiteIds = networkData.map { it.id }.toSet()

                if (savedWorksiteIds.isNotEmpty()) {
                    ensureActive()

                    try {
                        val networkWorksites =
                            networkDataSource.getWorksitesFlagsFormData(savedWorksiteIds)
                        saveAdditional(networkWorksites, statsUpdater)
                    } catch (e: Exception) {
                        appLogger.logDebug(e)
                    }
                }

                ensureActive()

                val maxRadius = liveRegion.radius

                val lastCoordinates = networkData.last().location.coordinates
                val furthestWorksiteRadius = lastCoordinates.radiusMiles(liveRegion) ?: maxRadius
                isOuterRegionReached = furthestWorksiteRadius >= maxRadius

                val distanceDetails = if (isOuterRegionReached) {
                    "all within $furthestWorksiteRadius/$maxRadius mi."
                } else {
                    "up to $furthestWorksiteRadius mi."
                }
                log("Cached ${deduplicateWorksites.size} ($savedCount/$initialCount) $distanceDetails around $locationDetails.")

                if (savedCount > maxCount) {
                    break
                }

                getLocation(incidentId)?.let {
                    val updatedRegion = liveRegion.copy(
                        latitude = it.first,
                        longitude = it.second,
                    )
                    if (liveRegion.isSignificantChange(updatedRegion)) {
                        liveRegion = updatedRegion
                        queryPage = 1
                        liveQueryAfter = null
                    }
                }
            }

            if (isPaused) {
                return@coroutineScope DownloadCountSpeed(savedCount, isSlowDownload)
            }
        } while (networkData.isNotEmpty() && !isOuterRegionReached)

        if (isOuterRegionReached) {
            val boundedRegionEncoded = try {
                Json.encodeToString(liveRegion)
            } catch (e: Exception) {
                appLogger.logException(e)
                ""
            }
            syncParameterDao.updatedBoundedParameters(
                incidentId,
                boundedRegionEncoded,
                syncStart,
            )
        }

        DownloadCountSpeed(savedCount, isSlowDownload)
    }

    // ~40000 Cases longer than 10 mins is reasonably slow
    private val slowSpeedDownload = 200f / 3

    private fun getMaxQueryCount(isAdditionalData: Boolean) = if (isAdditionalData) {
        if (deviceInspector.isLimitedDevice) 2000 else 6000
    } else {
        if (deviceInspector.isLimitedDevice) 3000 else 10000
    }

    private suspend fun cacheWorksitesCore(
        incidentId: Long,
        isPaused: Boolean,
        syncParameters: IncidentDataSyncParameters,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        var isSlowDownload: Boolean? = null
        var savedCount = 0

        val downloadSpeedTracker = CountTimeTracker()

        val timeMarkers = syncParameters.syncDataMeasures.core
        if (!timeMarkers.isDeltaSync) {
            val beforeResult = cacheWorksitesBefore(
                IncidentCacheStage.WorksitesCore,
                incidentId,
                isPaused,
                unmeteredDataCountThreshold = 9000,
                timeMarkers,
                statsUpdater,
                downloadSpeedTracker,
                isNetworkCountAdditive = false,
                { count: Int, before: Instant ->
                    networkDataSource.getWorksitesPageBefore(incidentId, count, before)
                },
                { worksites: List<NetworkWorksitePage> ->
                    saveWorksites(worksites, statsUpdater)
                },
            )

            if (isPaused) {
                return@coroutineScope beforeResult
            }

            isSlowDownload = beforeResult.isSlow
            savedCount = beforeResult.count
        }

        ensureActive()

        // TODO Deltas should account for deleted and/or reclassified

        val afterResult = cacheWorksitesAfter(
            IncidentCacheStage.WorksitesCore,
            incidentId,
            isPaused,
            unmeteredDataCountThreshold = 9000,
            timeMarkers,
            statsUpdater,
            downloadSpeedTracker,
            { count: Int, after: Instant ->
                networkDataSource.getWorksitesPageAfter(incidentId, count, after)
            },
            { worksites: List<NetworkWorksitePage> ->
                saveWorksites(worksites, statsUpdater)
            },
        )

        DownloadCountSpeed(
            savedCount + afterResult.count,
            isSlowDownload == true || afterResult.isSlow == true,
        )
    }

    private suspend fun <T, U> cacheWorksitesBefore(
        stage: IncidentCacheStage,
        incidentId: Long,
        isPaused: Boolean,
        unmeteredDataCountThreshold: Int,
        timeMarkers: IncidentDataSyncParameters.SyncTimeMarker,
        statsUpdater: IncidentDataPullStatsUpdater,
        downloadSpeedTracker: CountTimeTracker,
        isNetworkCountAdditive: Boolean,
        getNetworkData: suspend (Int, Instant) -> T,
        saveToDb: suspend (List<U>) -> Unit,
    ) where T : WorksiteDataResult<U>, U : WorksiteDataSubset = coroutineScope {
        var isSlowDownload: Boolean? = null

        fun log(message: String) = logStage(incidentId, stage, message)

        log("Downloading Worksites before")

        var queryCount = if (isPaused) 100 else 1000
        val maxQueryCount = getMaxQueryCount(stage == IncidentCacheStage.WorksitesAdditional)
        var beforeTimeMarker = timeMarkers.before
        var savedWorksiteIds = emptySet<Long>()
        var initialCount = -1
        var savedCount = 0

        do {
            ensureActive()

            val networkData = downloadSpeedTracker.time {
                // TODO Edge case where paging data breaks where Cases are equally updated_at
                val result = getNetworkData(
                    queryCount,
                    beforeTimeMarker,
                )

                if (isNetworkCountAdditive) {
                    statsUpdater.addDataCount(result.count ?: 0)
                } else {
                    if (initialCount < 0) {
                        initialCount = result.count ?: 0
                        statsUpdater.setDataCount(initialCount)
                    }
                }
                result.data ?: emptyList()
            }

            if (networkData.isEmpty()) {
                if (stage == IncidentCacheStage.WorksitesCore) {
                    syncParameterDao.updateUpdatedBefore(
                        incidentId,
                        IncidentDataSyncParameters.timeMarkerZero,
                    )
                } else {
                    syncParameterDao.updateAdditionalUpdatedBefore(
                        incidentId,
                        IncidentDataSyncParameters.timeMarkerZero,
                    )
                }

                log("Cached ($savedCount/$initialCount) Worksites before.")
            } else {
                isSlowDownload = downloadSpeedTracker.averageSpeed() < slowSpeedDownload
                speedMonitor.onSpeedChange(isSlowDownload)

                statsUpdater.addQueryCount(networkData.size)

                val deduplicateWorksites = networkData.filter {
                    !savedWorksiteIds.contains(it.id)
                }
                if (deduplicateWorksites.isEmpty()) {
                    val duplicateCount = networkData.size - deduplicateWorksites.size
                    log("$duplicateCount duplicate(s), before")
                    break
                }

                saveToDb(deduplicateWorksites)
                savedCount += deduplicateWorksites.size

                savedWorksiteIds = networkData.map { it.id }.toSet()

                queryCount = (queryCount * 2).coerceAtMost(maxQueryCount)
                beforeTimeMarker = networkData.last().updatedAt

                if (stage == IncidentCacheStage.WorksitesCore) {
                    syncParameterDao.updateUpdatedBefore(incidentId, beforeTimeMarker)
                } else {
                    syncParameterDao.updateAdditionalUpdatedBefore(incidentId, beforeTimeMarker)
                }

                log("Cached ${deduplicateWorksites.size} ($savedCount/$initialCount) before, back to $beforeTimeMarker")
            }

            if (isPaused) {
                return@coroutineScope DownloadCountSpeed(savedCount, isSlowDownload)
            }

            // TODO Account for low battery
            if (initialCount > unmeteredDataCountThreshold && !isNetworkUnmetered) {
                return@coroutineScope DownloadCountSpeed(savedCount, isSlowDownload)
            }
        } while (networkData.isNotEmpty())

        DownloadCountSpeed(savedCount, isSlowDownload)
    }

    private suspend fun <T, U> cacheWorksitesAfter(
        stage: IncidentCacheStage,
        incidentId: Long,
        isPaused: Boolean,
        unmeteredDataCountThreshold: Int,
        timeMarkers: IncidentDataSyncParameters.SyncTimeMarker,
        statsUpdater: IncidentDataPullStatsUpdater,
        downloadSpeedTracker: CountTimeTracker,
        getNetworkData: suspend (Int, Instant) -> T,
        saveToDb: suspend (List<U>) -> Unit,
    ) where T : WorksiteDataResult<U>, U : WorksiteDataSubset = coroutineScope {
        var isSlowDownload: Boolean? = null

        fun log(message: String) = logStage(incidentId, stage, message)

        var afterTimeMarker = timeMarkers.after

        log("Downloading delta starting at $afterTimeMarker")

        var queryCount = if (isPaused) 100 else 1000
        val maxQueryCount = getMaxQueryCount(stage == IncidentCacheStage.WorksitesAdditional)
        var savedWorksiteIds = emptySet<Long>()
        var initialCount = -1
        var savedCount = 0

        do {
            ensureActive()

            val networkData = downloadSpeedTracker.time {
                // TODO Edge case where paging data breaks where Cases are equally updated_at
                val result = getNetworkData(
                    queryCount,
                    afterTimeMarker,
                )
                if (initialCount < 0) {
                    initialCount = result.count ?: 0
                    statsUpdater.addDataCount(initialCount)
                }
                result.data ?: emptyList()
            }

            if (networkData.isEmpty()) {
                log("Cached $savedCount/$initialCount after. No Cases after $afterTimeMarker")
            } else {
                isSlowDownload = downloadSpeedTracker.averageSpeed() < slowSpeedDownload
                speedMonitor.onSpeedChange(isSlowDownload)

                statsUpdater.addQueryCount(networkData.size)

                val deduplicateWorksites = networkData.filter {
                    !savedWorksiteIds.contains(it.id)
                }
                if (deduplicateWorksites.isEmpty()) {
                    val duplicateCount = networkData.size - deduplicateWorksites.size
                    log("$duplicateCount duplicate(s) after")
                    break
                }

                saveToDb(deduplicateWorksites)
                savedCount += deduplicateWorksites.size

                savedWorksiteIds = networkData.map { it.id }.toSet()

                queryCount = (queryCount * 2).coerceAtMost(maxQueryCount)
                afterTimeMarker = networkData.last().updatedAt

                if (stage == IncidentCacheStage.WorksitesCore) {
                    syncParameterDao.updateUpdatedAfter(incidentId, afterTimeMarker)
                } else {
                    syncParameterDao.updateAdditionalUpdatedAfter(incidentId, afterTimeMarker)
                }

                log("Cached ${deduplicateWorksites.size} ($savedCount/$initialCount) after, up to $afterTimeMarker")
            }

            if (isPaused) {
                return@coroutineScope DownloadCountSpeed(savedCount, isSlowDownload)
            }

            // TODO Account for low battery
            if (initialCount > unmeteredDataCountThreshold && !isNetworkUnmetered) {
                return@coroutineScope DownloadCountSpeed(savedCount, isSlowDownload)
            }
        } while (networkData.isNotEmpty())

        DownloadCountSpeed(savedCount, isSlowDownload)
    }

    private suspend fun saveWorksites(
        networkWorksites: List<NetworkWorksitePage>,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        val worksites: List<WorksiteEntity>
        val flags: List<List<WorksiteFlagEntity>>
        val workTypes: List<List<WorkTypeEntity>>
        with(networkWorksites) {
            worksites = map { it.asEntity() }
            flags = map {
                it.flags.filter { flag -> flag.invalidatedAt == null }
                    .map(NetworkWorksiteFull.FlagShort::asEntity)
            }
            workTypes = map {
                it.newestWorkTypes.map(com.crisiscleanup.core.network.model.NetworkWorksiteFull.WorkTypeShort::asEntity)
            }
        }

        var offset = 0
        // TODO Provide configurable value. Account for device capabilities and/or OS version.
        val dbOperationLimit = 500
        val limit = dbOperationLimit.coerceAtLeast(100)
        while (offset < worksites.size) {
            val offsetEnd = (offset + limit).coerceAtMost(worksites.size)
            val worksiteSubset = worksites.slice(offset until offsetEnd)
            val workTypeSubset = workTypes.slice(offset until offsetEnd)
            worksiteDaoPlus.syncWorksites(
                worksiteSubset,
                workTypeSubset,
                statsUpdater.startedAt,
            )

            val flagSubset = flags.slice(offset until offsetEnd)
            worksiteDaoPlus.syncShortFlags(
                worksiteSubset,
                flagSubset,
            )

            statsUpdater.addSavedCount(worksiteSubset.size)

            offset += limit

            ensureActive()
        }
    }

    private suspend fun cacheAdditionalWorksiteData(
        incidentId: Long,
        isPaused: Boolean,
        syncParameters: IncidentDataSyncParameters,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        var isSlowDownload: Boolean? = null
        var savedCount = 0

        val downloadSpeedTracker = CountTimeTracker()

        val timeMarkers = syncParameters.syncDataMeasures.additional
        if (!timeMarkers.isDeltaSync) {
            val beforeResult = cacheWorksitesBefore(
                IncidentCacheStage.WorksitesAdditional,
                incidentId,
                isPaused,
                unmeteredDataCountThreshold = 3000,
                timeMarkers,
                statsUpdater,
                downloadSpeedTracker,
                isNetworkCountAdditive = true,
                { count: Int, before: Instant ->
                    networkDataSource.getWorksitesFlagsFormDataPageBefore(
                        incidentId,
                        count,
                        before,
                    )
                },
                { worksites: List<NetworkFlagsFormData> ->
                    saveAdditional(worksites, statsUpdater)
                },
            )

            if (isPaused) {
                return@coroutineScope beforeResult
            }

            isSlowDownload = beforeResult.isSlow
            savedCount = beforeResult.count
        }

        ensureActive()

        // TODO Deltas should account for deleted and/or reclassified

        val afterResult = cacheWorksitesAfter(
            IncidentCacheStage.WorksitesAdditional,
            incidentId,
            isPaused,
            unmeteredDataCountThreshold = 3000,
            timeMarkers,
            statsUpdater,
            downloadSpeedTracker,
            { count: Int, after: Instant ->
                networkDataSource.getWorksitesFlagsFormDataPageAfter(
                    incidentId,
                    count,
                    after,
                )
            },
            { worksites: List<NetworkFlagsFormData> ->
                saveAdditional(worksites, statsUpdater)
            },
        )

        DownloadCountSpeed(
            savedCount + afterResult.count,
            isSlowDownload == true || afterResult.isSlow == true,
        )
    }

    private suspend fun saveAdditional(
        networkData: List<NetworkFlagsFormData>,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        val worksiteIds: List<Long>
        val formData: List<List<WorksiteFormDataEntity>>
        val reportedBys: List<Long?>
        with(networkData) {
            worksiteIds = map(NetworkFlagsFormData::id)
            formData = map {
                it.formData.map(KeyDynamicValuePair::asWorksiteEntity)
            }
            reportedBys = map(NetworkFlagsFormData::reportedBy)
        }

        var offset = 0
        // TODO Provide configurable value. Account for device capabilities and/or OS version.
        val dbOperationLimit = 500
        val limit = dbOperationLimit.coerceAtLeast(100)
        while (offset < worksiteIds.size) {
            val offsetEnd = (offset + limit).coerceAtMost(worksiteIds.size)
            val worksiteIdsSubset = worksiteIds.slice(offset until offsetEnd)
            val formDataSubset = formData.slice(offset until offsetEnd)
            val reportedBysSubset = reportedBys.slice(offset until offsetEnd)
            // Flags saved previously
            worksiteDaoPlus.syncAdditionalData(
                worksiteIdsSubset,
                formDataSubset,
                reportedBysSubset,
            )

            statsUpdater.addSavedCount(worksiteIdsSubset.size)

            offset += limit

            ensureActive()
        }
    }

    override suspend fun resetIncidentSyncStats(incidentId: Long) {
        syncParameterDao.deleteSyncParameters(incidentId)
    }

    override suspend fun updateCachePreferences(preferences: IncidentWorksitesCachePreferences) {
        incidentCachePreferences.setPreferences(preferences)
    }

    private data class DownloadCountSpeed(
        val count: Int,
        val isSlow: Boolean? = null,
    )
}

enum class IncidentCacheStage {
    Start,
    Incidents,
    WorksitesBounded,
    WorksitesPreload,
    WorksitesCore,
    WorksitesAdditional,
    ActiveIncident,
    ActiveIncidentOrganization,
    End,
}

private data class IncidentDataSyncPlan(
    // May be a new Incident ID
    val incidentId: Long,
    val syncIncidents: Boolean,
    val syncSelectedIncident: Boolean,
    val syncActiveIncidentWorksites: Boolean,
    val syncWorksitesAdditional: Boolean,
    val restartCache: Boolean,
    val timestamp: Instant = Clock.System.now(),
) {
    val syncSelectedIncidentLevel by lazy {
        if (syncSelectedIncident) {
            1
        } else {
            0
        }
    }

    val syncWorksitesLevel by lazy {
        if (syncWorksitesAdditional) {
            2
        } else if (syncActiveIncidentWorksites) {
            1
        } else {
            0
        }
    }
}

private val EmptySyncPlan = IncidentDataSyncPlan(
    EmptyIncident.id,
    syncIncidents = false,
    syncSelectedIncident = false,
    syncActiveIncidentWorksites = false,
    syncWorksitesAdditional = false,
    restartCache = false,
)

private fun List<Double>.radiusMiles(boundedRegion: IncidentDataSyncParameters.BoundedRegion) =
    if (size == 2) {
        haversineDistance(
            get(1).radians,
            get(0).radians,
            boundedRegion.latitude.radians,
            boundedRegion.longitude.radians,
        ).kmToMiles
    } else {
        null
    }
