package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
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
import com.crisiscleanup.core.model.data.IncidentWorksitesCachePreferences
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkWorksitePage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface IncidentCacheRepository {
    val isSyncingActiveIncident: Flow<Boolean>

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
        cacheFullWorksites: Boolean,
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
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val deviceInspector: SyncCacheDeviceInspector,
    private val speedMonitor: DataDownloadSpeedMonitor,
    private val syncLogger: SyncLogger,
    private val appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Sync) private val appLogger: AppLogger,
) : IncidentCacheRepository, IncidentDataPullReporter {
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
        cacheFullWorksites: Boolean,
        restartCacheCheckpoint: Boolean,
        planTimeout: Duration,
    ): Boolean {
        val incidentIds = incidentsRepository.incidents.first()
            .map(Incident::id)
            .toSet()
        val selectedIncident = appPreferences.userData.first().selectedIncidentId
        val isIncidentCached = incidentIds.contains(selectedIncident)
        val submittedPlan = IncidentDataSyncPlan(
            selectedIncident,
            syncIncidents = forcePullIncidents || !isIncidentCached,
            syncSelectedIncident = cacheSelectedIncident || !isIncidentCached,
            syncActiveIncidentWorksites = cacheActiveIncidentWorksites,
            syncFullWorksites = cacheFullWorksites,
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

            val incidents = incidentsRepository.incidents.first()
            if (incidents.isEmpty()) {
                return@coroutineScope SyncResult.Error("Failed to sync Incidents")
            }
            val incidentIds = incidents.map(Incident::id).toSet()
            if (!incidentIds.contains(incidentId)) {
                return@coroutineScope SyncResult.Partial("Incident not found. Waiting for Incident select.")
            }

            val incidentName = incidents.first { it.id == incidentId }.name
            val shortWorksitesStatsUpdater = IncidentDataPullStatsUpdater {
                incidentDataPullStats.value = it
            }.apply {
                beginPull(
                    incidentId,
                    incidentName,
                    IncidentPullDataType.ShortWorksites,
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
                    IncidentCacheStage.Worksites,
                    "Restarting Worksites cache",
                )

                resetIncidentSyncStats(incidentId)
            }

            val syncStatsEntity = syncParameterDao.getSyncStats(incidentId)
            val preferencesBoundedRegion = IncidentDataSyncParameters.BoundedRegion(
                latitude = syncPreferences.regionLatitude,
                longitude = syncPreferences.regionLongitude,
                radius = syncPreferences.regionRadiusMiles,
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
                        isPaused = syncPreferences.isPaused,
                        isMyLocationBounded = syncPreferences.isRegionMyLocation,
                        preferencesBoundedRegion = preferencesBoundedRegion,
                        savedBoundedRegion = syncStats.boundedRegion,
                        syncStats.boundedSyncedAt,
                        shortWorksitesStatsUpdater,
                    )
                } else {
                    partialSyncReasons.add("Incomplete bounded region. Skipping Worksites sync.")
                }
            } else {
                val shortResult = cacheShortWorksites(
                    incidentId,
                    syncPreferences.isPaused,
                    syncStats,
                    shortWorksitesStatsUpdater,
                )
                isSlowDownload = shortResult.isSlowDownload
            }

            ensureActive()
            shortWorksitesStatsUpdater.endPull()

            // TODO Alert elsewhere by subscribing to DataDownloadSpeedMonitor and IncidentWorksitesCachePreferences distinctUntilChanged (speed and isPaused)

            if (syncPreferences.isPaused) {
                if (isSlowDownload) {
                    partialSyncReasons.add("Worksite downloads are paused")
                    skipWorksiteCaching = true
                }
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

                logStage(incidentId, IncidentCacheStage.FullWorksites)

                val fullWorksitesStatsUpdater = IncidentDataPullStatsUpdater {
                    incidentDataPullStats.value = it
                }.apply {
                    beginPull(
                        incidentId,
                        incidentName,
                        IncidentPullDataType.FullWorksites,
                    )
                }

                cacheFullWorksites(
                    incidentId,
                    syncPreferences.isPaused,
                    syncStats,
                    fullWorksitesStatsUpdater,
                )

                ensureActive()
                fullWorksitesStatsUpdater.endPull()
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

    private suspend fun cacheBoundedWorksites(
        incidentId: Long,
        isPaused: Boolean,
        isMyLocationBounded: Boolean,
        preferencesBoundedRegion: IncidentDataSyncParameters.BoundedRegion,
        savedBoundedRegion: IncidentDataSyncParameters.BoundedRegion?,
        syncedAt: Instant,
        statsUpdater: IncidentDataPullStatsUpdater,
        similarBoundsTimeout: Duration = 5.minutes,
    ) = coroutineScope {
        var boundedRegion = preferencesBoundedRegion

        if (isMyLocationBounded) {
            // TODO Query current coordinates, with timeout, rather than using cached
            locationProvider.coordinates?.let {
                boundedRegion = boundedRegion.copy(
                    latitude = it.first,
                    longitude = it.second,
                )
            }

            if (locationProvider.coordinates == null) {
                logStage(
                    incidentId,
                    IncidentCacheStage.Worksites,
                    "Current user location is not cached. Falling back to saved location.",
                )
            }
        }

        if (Clock.System.now() - syncedAt < similarBoundsTimeout &&
            savedBoundedRegion?.isSimilar(boundedRegion) == true
        ) {
            logStage(
                incidentId,
                IncidentCacheStage.Worksites,
                "Skipping caching of bounded Worksites. Insignificant bounds change.",
            )
        } else {
            logStage(
                incidentId,
                IncidentCacheStage.Worksites,
                "Caching bounded Worksites",
            )

            // TODO Try at least once and determine if speed is sufficient.
            //      Alert if speed is sufficient regardless if paused.

//            val networkWorksites = networkDataSource.getWorksites(queryIds)
//            networkWorksites?.let { worksites ->
//                if (worksites.isNotEmpty()) {
//                    val entities = worksites.map(NetworkWorksiteFull::asEntities)
//                    worksiteDaoPlus.syncWorksites(entities, syncStartedAt)
//
//                    ensureActive()
//                }
//            }
        }
    }

    // ~50000 Cases longer than 10 mins is a reasonable threshold
    private val slowSpeedDownload = 250f / 3

    private val maxQueryCount by lazy {
        if (deviceInspector.isLimitedDevice) 3000 else 10000
    }
    private val maxFullQueryCount by lazy {
        if (deviceInspector.isLimitedDevice) 2000 else 6000
    }

    private suspend fun cacheShortWorksites(
        incidentId: Long,
        isPaused: Boolean,
        syncParameters: IncidentDataSyncParameters,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        var isSlowDownload: Boolean?

        val downloadSpeedTracker = CountTimeTracker()

        val timeMarkers = syncParameters.syncDataMeasures.short
        if (!timeMarkers.isDeltaSync) {
            isSlowDownload = cacheShortWorksitesBefore(
                incidentId,
                isPaused,
                timeMarkers,
                statsUpdater,
                downloadSpeedTracker,
            )
            if (isPaused && isSlowDownload == true) {
                return@coroutineScope SyncStageResult(
                    isSuccess = false,
                    isSlowDownload = true,
                )
            }
        }

        ensureActive()

        // TODO Deltas must account for deleted and/or reclassified

        isSlowDownload = cacheShortWorksitesAfter(
            incidentId,
            isPaused,
            timeMarkers,
            statsUpdater,
            downloadSpeedTracker,
        )

        SyncStageResult(!isPaused, isSlowDownload = isSlowDownload == true)
    }

    private suspend fun cacheShortWorksitesBefore(
        incidentId: Long,
        isPaused: Boolean,
        timeMarkers: IncidentDataSyncParameters.SyncTimeMarker,
        statsUpdater: IncidentDataPullStatsUpdater,
        downloadSpeedTracker: CountTimeTracker,
    ) = coroutineScope {
        var isSlowDownload: Boolean? = null

        logStage(
            incidentId,
            IncidentCacheStage.Worksites,
            "Downloading Worksites before",
        )

        var queryCount = if (isPaused) 100 else 1000
        var beforeTimeMarker = timeMarkers.before
        var savedWorksiteIds = emptySet<Long>()
        var initialCount = -1
        var savedCount = 0

        do {
            ensureActive()

            val networkWorksites = downloadSpeedTracker.time {
                // TODO Edge case where paging data breaks where Cases are equally updated_at
                val result = networkDataSource.getWorksitesPageBefore(
                    incidentId,
                    queryCount,
                    beforeTimeMarker,
                )
                if (initialCount < 0) {
                    initialCount = result.count ?: 0
                    statsUpdater.setDataCount(initialCount)
                }
                result.results ?: emptyList()
            }

            if (networkWorksites.isEmpty()) {
                syncParameterDao.updateUpdatedBefore(
                    incidentId,
                    IncidentDataSyncParameters.timeMarkerZero,
                )

                logStage(
                    incidentId,
                    IncidentCacheStage.Worksites,
                    "Cached ($savedCount/$initialCount) Worksites before.",
                )
            } else {
                isSlowDownload = downloadSpeedTracker.averageSpeed() < slowSpeedDownload
                speedMonitor.onSpeedChange(isSlowDownload)

                statsUpdater.addQueryCount(networkWorksites.size)

                val deduplicateWorksites = networkWorksites.filter {
                    !savedWorksiteIds.contains(it.id)
                }
                if (deduplicateWorksites.isEmpty()) {
                    val duplicateCount = networkWorksites.size - deduplicateWorksites.size
                    logStage(
                        incidentId,
                        IncidentCacheStage.Worksites,
                        "$duplicateCount duplicate(s), before",
                    )
                    break
                }

                saveToDb(
                    deduplicateWorksites,
                    statsUpdater.startedAt,
                    statsUpdater,
                )
                savedCount += deduplicateWorksites.size

                savedWorksiteIds = networkWorksites.map(NetworkWorksitePage::id).toSet()

                queryCount = (queryCount * 2).coerceAtMost(maxQueryCount)
                beforeTimeMarker = networkWorksites.last().updatedAt

                syncParameterDao.updateUpdatedBefore(incidentId, beforeTimeMarker)

                logStage(
                    incidentId,
                    IncidentCacheStage.Worksites,
                    "Cached ${deduplicateWorksites.size} ($savedCount/$initialCount) before, back to $beforeTimeMarker",
                )
            }

            if (isPaused && isSlowDownload == true) {
                return@coroutineScope true
            }
        } while (networkWorksites.isNotEmpty())

        isSlowDownload
    }

    private suspend fun cacheShortWorksitesAfter(
        incidentId: Long,
        isPaused: Boolean,
        timeMarkers: IncidentDataSyncParameters.SyncTimeMarker,
        statsUpdater: IncidentDataPullStatsUpdater,
        downloadSpeedTracker: CountTimeTracker,
    ) = coroutineScope {
        var isSlowDownload: Boolean? = null

        var afterTimeMarker = timeMarkers.after

        logStage(
            incidentId,
            IncidentCacheStage.Worksites,
            "Downloading delta starting at $afterTimeMarker",
        )

        var queryCount = if (isPaused) 100 else 1000
        var savedWorksiteIds = emptySet<Long>()
        var initialCount = -1
        var savedCount = 0

        do {
            ensureActive()

            val networkWorksites = downloadSpeedTracker.time {
                // TODO Edge case where paging data breaks where Cases are equally updated_at
                val result = networkDataSource.getWorksitesPageAfter(
                    incidentId,
                    queryCount,
                    afterTimeMarker,
                )
                if (initialCount < 0) {
                    initialCount = result.count ?: 0
                    statsUpdater.addDataCount(initialCount)
                }
                result.results ?: emptyList()
            }

            if (networkWorksites.isEmpty()) {
                logStage(
                    incidentId,
                    IncidentCacheStage.Worksites,
                    "Cached $savedCount/$initialCount after. No Cases after $afterTimeMarker",
                )
            } else {
                isSlowDownload = downloadSpeedTracker.averageSpeed() < slowSpeedDownload
                speedMonitor.onSpeedChange(isSlowDownload)

                statsUpdater.addQueryCount(networkWorksites.size)

                val deduplicateWorksites = networkWorksites.filter {
                    !savedWorksiteIds.contains(it.id)
                }
                if (deduplicateWorksites.isEmpty()) {
                    val duplicateCount = networkWorksites.size - deduplicateWorksites.size
                    logStage(
                        incidentId,
                        IncidentCacheStage.Worksites,
                        "$duplicateCount duplicate(s) after",
                    )
                    break
                }

                saveToDb(
                    deduplicateWorksites,
                    statsUpdater.startedAt,
                    statsUpdater,
                )
                savedCount += deduplicateWorksites.size

                savedWorksiteIds = networkWorksites.map(NetworkWorksitePage::id).toSet()

                queryCount = (queryCount * 2).coerceAtMost(maxQueryCount)
                afterTimeMarker = networkWorksites.last().updatedAt

                syncParameterDao.updateUpdatedAfter(incidentId, afterTimeMarker)

                logStage(
                    incidentId,
                    IncidentCacheStage.Worksites,
                    "Cached ${deduplicateWorksites.size} ($savedCount/$initialCount) after, up to $afterTimeMarker",
                )
            }

            if (isPaused && isSlowDownload == true) {
                return@coroutineScope true
            }
        } while (networkWorksites.isNotEmpty())

        isSlowDownload
    }

    private suspend fun saveToDb(
        networkWorksites: List<NetworkWorksitePage>,
        syncStart: Instant,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        val worksites: List<WorksiteEntity>
        val flags: List<List<WorksiteFlagEntity>>
        val workTypes: List<List<WorkTypeEntity>>
        with(networkWorksites) {
            worksites = map { it.asEntity() }
            flags = map {
                it.flags.filter { flag -> flag.invalidatedAt == null }
                    .map(com.crisiscleanup.core.network.model.NetworkWorksiteFull.FlagShort::asEntity)
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
                syncStart,
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

    private suspend fun cacheFullWorksites(
        incidentId: Long,
        isPaused: Boolean,
        syncStatsInitial: IncidentDataSyncParameters,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        // TODO Try at least once and determine if speed is sufficient.
        //      Alert if paused and speed is sufficient.

//        val worksites = networkDataSource.getWorksitesFlagsFormDataPage(
//            incidentId,
//            pageCount,
//            pageIndex + 1,
//            updatedAtAfter = updatedAfter,
//        )

//        with(cachedData.secondaryData) {
//            val worksitesIds = map(NetworkFlagsFormData::id)
//            val formData = map {
//                it.formData.map(KeyDynamicValuePair::asWorksiteEntity)
//            }
//            val reportedBys = map(NetworkFlagsFormData::reportedBy)
//            saveToDb(
//                worksitesIds,
//                formData,
//                reportedBys,
//                statsUpdater,
//            )
//        }

        // TODO Require
        //      - battery power is > X%
        //      - Wifi
    }

    private suspend fun saveToDb(
        worksiteIds: List<Long>,
        formData: List<List<WorksiteFormDataEntity>>,
        reportedBys: List<Long?>,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        var offset = 0
        // TODO Provide configurable value. Account for device capabilities and/or OS version.
        val dbOperationLimit = 500
        val limit = dbOperationLimit.coerceAtLeast(100)
        while (offset < worksiteIds.size) {
            val offsetEnd = (offset + limit).coerceAtMost(worksiteIds.size)
            val worksiteIdsSubset = worksiteIds.slice(offset until offsetEnd)
            val formDataSubset = formData.slice(offset until offsetEnd)
            val reportedBysSubset = reportedBys.slice(offset until offsetEnd)
            // Flags is saved in short
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
        syncParameterDao.deleteSyncStats(incidentId)
    }

    override suspend fun updateCachePreferences(preferences: IncidentWorksitesCachePreferences) {
        incidentCachePreferences.setPreferences(preferences)
    }

    private enum class IncidentCacheStage {
        Start,
        Incidents,
        Worksites,
        FullWorksites,
        ActiveIncident,
        ActiveIncidentOrganization,
        End,
    }

    private data class SyncStageResult(
        val isSuccess: Boolean = false,
        val isSlowDownload: Boolean = false,
    )
}

private data class IncidentDataSyncPlan(
    // May be a new Incident ID
    val incidentId: Long,
    val syncIncidents: Boolean,
    val syncSelectedIncident: Boolean,
    val syncActiveIncidentWorksites: Boolean,
    val syncFullWorksites: Boolean,
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
        if (syncFullWorksites) {
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
    syncFullWorksites = false,
    restartCache = false,
)
