package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.lang.Integer.max
import java.lang.Integer.min
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstWorksitesRepository @Inject constructor(
    private val worksiteNetworkDataSource: CrisisCleanupNetworkDataSource,
    private val worksitesSyncStatsDao: WorksitesSyncStatsDao,
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val appLogger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : WorksitesRepository {
    // TODO Defer to provider instead. So amount can vary according to (WifiManager) signal level or equivalent.
    /**
     * Number of worksites per query page.
     */
    var worksitesQueryBasePageAmount: Int = 100

    /**
     * Amount of DB operations per transaction when caching worksites short data
     */
    var worksitesDbOperationAmount: Int = 300

    override var isLoading = MutableStateFlow(false)
        private set

    override suspend fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeMin: Double,
        latitudeMax: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int,
        offset: Int,
    ): Flow<List<WorksiteMapMark>> = withContext(ioDispatcher) {
        return@withContext worksiteDaoPlus.getWorksitesMapVisual(
            incidentId,
            latitudeMin,
            latitudeMax,
            longitudeLeft,
            longitudeRight,
            limit,
            offset
        )
            .map { it.map(PopulatedWorksiteMapVisual::asExternalModel) }
    }

    override fun getWorksites(incidentId: Long, limit: Int, offset: Int): Flow<List<Worksite>> {
        return worksiteDao.getWorksites(incidentId, limit, offset)
            .map { it.map(PopulatedWorksite::asExternalModel) }
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
        val syncStatsQuery = worksitesSyncStatsDao.getSyncStats(incidentId).first()
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

        // TODO Make value configurable and responsive to network speed, battery, ...
        val syncedCount = if (count > 100)
        // TODO This is short synced count not the full synced count. Revisit endpoint when paging is reliable and needs differentiation.
            syncWorksitesShortData(incidentId, syncStart)
        else syncWorksitesPagedData(incidentId, syncStart, count)

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

            val worksites = worksitesRequest.results?.map { it.asEntity(incidentId) } ?: emptyList()
            pagedCount += saveToDb(incidentId, worksites, syncStart)
            offset += limit
        }

        return pagedCount
    }

    private suspend fun saveToDb(
        incidentId: Long,
        worksites: List<WorksiteEntity>,
        syncStart: Instant,
    ): Int {
        var offset = 0
        val limit = max(worksitesDbOperationAmount, 10)
        var pagedCount = 0
        while (offset < worksites.size) {
            val offsetEnd = min(offset + limit, worksites.size)
            val subset = worksites.slice(offset until offsetEnd)
            worksiteDaoPlus.syncExternalWorksites(
                incidentId,
                subset,
                syncStart,
            )

            pagedCount += subset.size
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

        val worksites = worksitesRequest.results?.map { it.asEntity(incidentId) } ?: emptyList()
        return saveToDb(incidentId, worksites, syncStart)
    }

    // TODO Write tests
    // TODO Split force into two arguments.
    //      - Clear should delete all worksites data and sync.
    //      - Soft force should sync deltas since last.
    override suspend fun refreshWorksites(
        incidentId: Long,
        force: Boolean
    ) = withContext(ioDispatcher) {
        if (incidentId == EmptyIncident.id) {
            return@withContext
        }

        // TODO Enforce single process syncing per incident

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