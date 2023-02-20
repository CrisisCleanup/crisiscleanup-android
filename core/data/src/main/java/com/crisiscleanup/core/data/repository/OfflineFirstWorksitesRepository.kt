package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.util.IncidentWorksitesDataPullStats
import com.crisiscleanup.core.data.util.WorksitesDataPullReporter
import com.crisiscleanup.core.data.util.WorksitesDataPullStatsUpdater
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import com.crisiscleanup.core.database.model.*
import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.tryThrowException
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksiteFull.WorkType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.lang.Integer.max
import java.lang.Integer.min
import javax.inject.Inject
import javax.inject.Singleton

// TODO Clear sync stats on logout? Or is it more efficient to keep? Are there differences in data when different accounts request data?

@Singleton
class OfflineFirstWorksitesRepository @Inject constructor(
    private val worksiteNetworkDataSource: CrisisCleanupNetworkDataSource,
    private val worksitesSyncStatsDao: WorksitesSyncStatsDao,
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val memoryStats: AppMemoryStats,
    private val appVersionProvider: AppVersionProvider,
    private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : WorksitesRepository, WorksitesDataPullReporter {
    // TODO Defer to provider instead. So amount can vary according to (WifiManager) signal level or equivalent. Must track request timeouts and give feedback or adjust.
    /**
     * Number of worksites per query page.
     */
    var worksitesQueryBasePageAmount: Int = 20

    /**
     * Amount of DB operations per transaction when caching worksites short data
     */
    var worksitesDbOperationAmount: Int = 500

    private val allWorksitesMemoryThreshold = 100

    override var isLoading = MutableStateFlow(false)
        private set

    override val worksitesDataPullStats = MutableStateFlow(IncidentWorksitesDataPullStats())

    init {
        logger.tag = "worksites-repo"
    }

    override fun streamWorksites(incidentId: Long, limit: Int, offset: Int): Flow<List<Worksite>> {
        return worksiteDao.streamWorksites(incidentId, limit, offset)
            .map { it.map(PopulatedWorksite::asExternalModel) }
    }

    override fun streamIncidentWorksitesCount(id: Long): Flow<Int> {
        return worksiteDao.streamWorksitesCount(id)
    }

    override suspend fun streamWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int,
        offset: Int,
    ): Flow<List<WorksiteMapMark>> = withContext(ioDispatcher) {
        return@withContext worksiteDaoPlus.streamWorksitesMapVisual(
            incidentId,
            latitudeSouth,
            latitudeNorth,
            longitudeLeft,
            longitudeRight,
            limit,
            offset
        )
            .map { it.map(PopulatedWorksiteMapVisual::asExternalModel) }
    }

    override fun getWorksitesMapVisual(
        incidentId: Long,
        limit: Int,
        offset: Int
    ): List<WorksiteMapMark> {
        return worksiteDao.getWorksitesMapVisual(incidentId, limit, offset)
            .map(PopulatedWorksiteMapVisual::asExternalModel)
    }

    override fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
        limit: Int,
        offset: Int
    ): List<WorksiteMapMark> {
        return worksiteDao.getWorksitesMapVisual(
            incidentId,
            latitudeSouth,
            latitudeNorth,
            longitudeWest,
            longitudeEast,
            limit,
            offset
        )
            .map(PopulatedWorksiteMapVisual::asExternalModel)
    }

    override fun getWorksitesCount(incidentId: Long): Int =
        worksiteDao.getWorksitesCount(incidentId)

    override fun getWorksitesSyncStats(incidentId: Long): WorksitesSyncStats? {
        return worksitesSyncStatsDao.getSyncStats(incidentId).firstOrNull()?.asExternalModel()
    }

    private suspend fun networkWorksitesCount(
        incidentId: Long,
        throwOnEmpty: Boolean = false
    ): Int {
        val worksitesCountResult =
            worksiteNetworkDataSource.getWorksitesCount(incidentId)
        tryThrowException(worksitesCountResult.errors)

        val count = worksitesCountResult.count ?: 0
        if (throwOnEmpty && count == 0) {
            throw Exception("Backend is reporting no worksites for incident $incidentId")
        }

        return count
    }

    private suspend fun querySyncStats(
        incidentId: Long,
        syncStart: Instant,
    ): WorksitesSyncStats {
        val syncStatsQuery = worksitesSyncStatsDao.getSyncStats(incidentId)
        if (syncStatsQuery.isNotEmpty()) {
            return syncStatsQuery.first().asExternalModel()
        }

        val worksitesCount = networkWorksitesCount(incidentId, true)
        val syncStats = WorksitesSyncStats(
            incidentId,
            syncStart = syncStart,
            worksitesCount = worksitesCount,
            syncAttempt = SyncAttempt(0, 0, 0),
            appBuildVersionCode = 0,
        )
        worksitesSyncStatsDao.upsertStats(syncStats.asEntity())

        return syncStats
    }

    private suspend fun syncWorksitesDataStat(
        incidentId: Long,
        syncStart: Instant,
    ): WorksitesSyncStats {
        val statsUpdater = WorksitesDataPullStatsUpdater(
            updatePullStats = { stats -> worksitesDataPullStats.value = stats }
        ).also {
            it.beginPull(incidentId)
            it.beginRequest()
        }
        try {
            return syncWorksitesData(incidentId, syncStart, statsUpdater)
        } finally {
            statsUpdater.endPull()
        }
    }

    private suspend fun syncWorksitesData(
        incidentId: Long,
        syncStart: Instant,
        statsUpdater: WorksitesDataPullStatsUpdater,
    ): WorksitesSyncStats {
        val count = networkWorksitesCount(incidentId, true)

        statsUpdater.updateWorksitesCount(count)

        // TODO Make value configurable and responsive to device resources, network speed, battery, ...
        val syncedCount =
            if (memoryStats.availableMemory >= allWorksitesMemoryThreshold) {
                // TODO This is short synced count not the full synced count. Revisit endpoint when paging is reliable and needs differentiation.
                syncWorksitesShortData(incidentId, syncStart, statsUpdater)
            } else {
                // TODO Alert the device is lacking and the experience will be degraded
                statsUpdater.setPagingRequest()

                logger.logDebug("Paging worksites request due to constrained memory ${memoryStats.availableMemory}")

                syncWorksitesPagedData(incidentId, syncStart, count, statsUpdater)
            }

        val syncSeconds = syncStart.epochSeconds
        return WorksitesSyncStats(
            incidentId,
            syncStart,
            count,
            syncedCount,
            SyncAttempt(syncSeconds, syncSeconds, 0),
            appVersionProvider.versionCode,
        )
    }

    private suspend fun syncWorksitesPagedData(
        incidentId: Long,
        syncStart: Instant,
        worksitesCount: Int,
        statsUpdater: WorksitesDataPullStatsUpdater,
    ): Int = coroutineScope {
        var offset = 0
        val limit = max(worksitesQueryBasePageAmount, 5)
        var pagedCount = 0
        while (offset < worksitesCount) {
            val worksitesRequest = worksiteNetworkDataSource.getWorksites(incidentId, limit, offset)
            tryThrowException(worksitesRequest.errors)

            val requestCount = worksitesRequest.count ?: 0
            if (requestCount <= 0) {
                break
            }

            ensureActive()

            statsUpdater.updateRequestedCount(offset + requestCount)

            worksitesRequest.results?.let { list ->
                val worksites = list.map { it.asEntity(incidentId) }
                val workTypes = list.map { it.workTypes.map(WorkType::asEntity) }
                pagedCount += saveToDb(incidentId, worksites, workTypes, syncStart, statsUpdater)
            }

            offset += limit
        }

        statsUpdater.endRequest()

        return@coroutineScope pagedCount
    }

    private suspend fun saveToDb(
        incidentId: Long,
        worksites: List<WorksiteEntity>,
        workTypes: List<List<WorkTypeEntity>>,
        syncStart: Instant,
        statsUpdater: WorksitesDataPullStatsUpdater,
    ): Int = coroutineScope {
        var offset = 0
        val limit = max(worksitesDbOperationAmount, 10)
        var pagedCount = 0
        while (offset < worksites.size) {
            val offsetEnd = min(offset + limit, worksites.size)
            val worksiteSubset = worksites.slice(offset until offsetEnd)
            val workTypeSubset = workTypes.slice(offset until offsetEnd)
            worksiteDaoPlus.syncWorksites(
                incidentId,
                worksiteSubset,
                workTypeSubset,
                syncStart,
            )

            statsUpdater.addSavedCount(worksiteSubset.size)

            pagedCount += worksiteSubset.size

            offset += limit

            ensureActive()
        }
        return@coroutineScope pagedCount
    }

    private suspend fun syncWorksitesShortData(
        incidentId: Long,
        syncStart: Instant,
        statsUpdater: WorksitesDataPullStatsUpdater,
    ): Int = coroutineScope {
        val worksitesRequest = worksiteNetworkDataSource.getWorksitesAll(incidentId, null)
        tryThrowException(worksitesRequest.errors)

        ensureActive()

        statsUpdater.endRequest()
        val requestCount = worksitesRequest.count ?: 0
        statsUpdater.updateRequestedCount(requestCount)

        worksitesRequest.results?.let { list ->
            val worksites = list.map { it.asEntity(incidentId) }
            val workTypes =
                list.map { it.workTypes.map(NetworkWorksiteFull.WorkTypeShort::asEntity) }
            return@coroutineScope saveToDb(
                incidentId,
                worksites,
                workTypes,
                syncStart,
                statsUpdater,
            )
        }

        return@coroutineScope 0
    }

    // TODO Write tests
    // TODO Split force into two arguments.
    //      - Clear should delete all worksites data and sync.
    //      - Soft force should sync deltas since last.
    override suspend fun refreshWorksites(
        incidentId: Long,
        force: Boolean
    ) = coroutineScope {
        if (incidentId == EmptyIncident.id) {
            return@coroutineScope
        }

        // TODO Enforce single process syncing per incident since this may be very long running

        isLoading.value = true
        try {
            val syncStart = Clock.System.now()

            var syncStats = querySyncStats(incidentId, syncStart)

            val savedWorksitesCount = worksiteDao.getWorksitesCount(incidentId)

            val syncFull = force ||
                    savedWorksitesCount < syncStats.worksitesCount ||
                    syncStats.isDataVersionOutdated
            if (syncFull) {
                // TODO Is it possible skip saved worksites and only pull missing worksites or start from syncStats.pagedCount?
                syncStats = syncWorksitesDataStat(incidentId, syncStart)
            }

            if (syncFull || syncStats.syncAttempt.shouldSyncPassively()) {
                // TODO Compare last successful with syncStart and determine if extra syncing is necessary.
                //      Update syncStats accordingly.
            }

            worksitesSyncStatsDao.upsertStats(syncStats.asEntity())
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw (e)
            } else {
                // TODO Update sync stats and attempt
                // TODO User feedback?
                logger.logException(e)
            }
        } finally {
            isLoading.value = false
        }
    }
}