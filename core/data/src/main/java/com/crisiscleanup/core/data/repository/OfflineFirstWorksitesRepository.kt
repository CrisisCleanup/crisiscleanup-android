package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.asEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.model.data.WorksitesSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.tryThrowException
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksiteFull.WorkType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
    private val appLogger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : WorksitesRepository {
    // TODO Defer to provider instead. So amount can vary according to (WifiManager) signal level or equivalent. Must track request timeouts and give feedback or adjust.
    /**
     * Number of worksites per query page.
     */
    var worksitesQueryBasePageAmount: Int = 20

    /**
     * Amount of DB operations per transaction when caching worksites short data
     */
    var worksitesDbOperationAmount: Int = 300

    private val allWorksitesMemoryThreshold = 100

    override var isLoading = MutableStateFlow(false)
        private set

    init {
        appLogger.tag = "worksites-repo"
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
        )
        worksitesSyncStatsDao.upsertStats(syncStats.asEntity())

        return syncStats
    }

    private suspend fun syncWorksitesData(
        incidentId: Long,
        syncStart: Instant,
    ): WorksitesSyncStats {
        val count = networkWorksitesCount(incidentId, true)

        // TODO Make value configurable and responsive to device resources, network speed, battery, ...
        val syncedCount =
            if (count > 300 && memoryStats.availableMemory >= allWorksitesMemoryThreshold) {
                // TODO This is short synced count not the full synced count. Revisit endpoint when paging is reliable and needs differentiation.
                syncWorksitesShortData(incidentId, syncStart)
            } else {
                appLogger.logDebug("Paging worksites request due to constrained memory ${memoryStats.availableMemory}")
                // TODO Alert the device is lacking and the experience will be degraded
                syncWorksitesPagedData(incidentId, syncStart, count)
            }

        val syncSeconds = syncStart.epochSeconds
        return WorksitesSyncStats(
            incidentId,
            syncStart,
            count,
            syncedCount,
            SyncAttempt(syncSeconds, syncSeconds, 0)
        )
    }

    private suspend fun syncWorksitesPagedData(
        incidentId: Long,
        syncStart: Instant,
        worksitesCount: Int,
    ): Int {
        var offset = 0
        val limit = max(worksitesQueryBasePageAmount, 5)
        var pagedCount = 0
        while (offset < worksitesCount) {
            val worksitesRequest = worksiteNetworkDataSource.getWorksites(incidentId, limit, offset)
            tryThrowException(worksitesRequest.errors)

            worksitesRequest.results?.let { list ->
                val worksites = list.map { it.asEntity(incidentId) }
                val workTypes = list.map { it.workTypes.map(WorkType::asEntity) }
                pagedCount += saveToDb(incidentId, worksites, workTypes, syncStart)
                offset += limit
            }
        }

        return pagedCount
    }

    private suspend fun saveToDb(
        incidentId: Long,
        worksites: List<WorksiteEntity>,
        workTypes: List<List<WorkTypeEntity>>,
        syncStart: Instant,
    ): Int {
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

            pagedCount += worksiteSubset.size
            offset += limit
        }
        return pagedCount
    }

    private suspend fun syncWorksitesShortData(
        incidentId: Long,
        syncStart: Instant,
    ): Int {
        val worksitesRequest = worksiteNetworkDataSource.getWorksitesAll(incidentId, null)
        tryThrowException(worksitesRequest.errors)

        worksitesRequest.results?.let { list ->
            val worksites = list.map { it.asEntity(incidentId) }
            val workTypes =
                list.map { it.workTypes.map(NetworkWorksiteFull.WorkTypeShort::asEntity) }
            return saveToDb(incidentId, worksites, workTypes, syncStart)
        }

        return 0
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

            val syncFull = force || savedWorksitesCount < syncStats.worksitesCount
            if (syncFull || syncStats.syncAttempt.shouldSyncPassively()) {
                // TODO Is it possible skip saved worksites and only pull missing worksites or start from syncStats.pagedCount?
                syncStats = syncWorksitesData(incidentId, syncStart)
            }

            if (syncFull || syncStats.syncAttempt.shouldSyncPassively()) {
                // TODO Compare last successful with syncStart and determine if extra syncing is necessary
            }

            worksitesSyncStatsDao.upsertStats(syncStats.asEntity())
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw (e)
            } else {
                // TODO User feedback?
                appLogger.logException(e)
            }
        } finally {
            isLoading.value = false
        }
    }
}