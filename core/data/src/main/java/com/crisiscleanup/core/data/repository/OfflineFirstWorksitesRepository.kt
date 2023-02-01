package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.UuidGenerator
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.asEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksitesSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.lang.Integer.max
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstWorksitesRepository @Inject constructor(
    private val worksiteNetworkDataSource: CrisisCleanupNetworkDataSource,
    private val worksitesSyncStatsDao: WorksitesSyncStatsDao,
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val uuidGenerator: UuidGenerator,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : WorksitesRepository {
    // TODO Defer to provider instead. So amount can vary according to (WifiManager) signal level or equivalent.
    var worksitesQueryBasePageAmount: Int = 100

    override var isLoading = MutableStateFlow(false)
        private set

    override fun getWorksites(incidentId: Long): Flow<List<Worksite>> {
        return worksiteDao.getWorksites(incidentId)
            .map { it.map(PopulatedWorksite::asExternalModel) }
    }

    private suspend fun networkWorksitesCount(incidentId: Long): Int {
        val worksitesCountResult =
            worksiteNetworkDataSource.getWorksitesCount(incidentId)
        if (worksitesCountResult.errors?.isNotEmpty() == true) {
            throw Exception(
                NetworkCrisisCleanupApiError.collapseMessages(
                    worksitesCountResult.errors!!
                )
            )
        }
        return worksitesCountResult.count ?: 0
    }

    private suspend fun querySyncStats(
        incidentId: Long,
        syncStart: Instant,
    ): WorksitesSyncStats {
        val syncStatsQuery = worksitesSyncStatsDao.getSyncStats(incidentId).first()
        if (syncStatsQuery.isNotEmpty()) {
            return syncStatsQuery.first().asExternalModel()
        }

        val worksitesCount = networkWorksitesCount(incidentId)
        if (worksitesCount > 0) {
            val syncStats = WorksitesSyncStats(
                incidentId,
                syncStart = syncStart,
                worksitesCount = worksitesCount,
                syncAttempt = SyncAttempt(0, 0, 0),
            )
            worksitesSyncStatsDao.upsertStats(syncStats.asEntity())

            return syncStats
        }

        throw Exception("Backend is reporting no worksites for incident $incidentId")
    }

    private suspend fun syncWorksitesData(
        incidentId: Long,
        syncStart: Instant,
    ): WorksitesSyncStats {
        val worksitesCount = networkWorksitesCount(incidentId)
        var offset = 0
        val limit = max(worksitesQueryBasePageAmount, 5)
        var pagedCount = 0
        while (offset < worksitesCount) {
            val worksitesRequest = worksiteNetworkDataSource.getWorksites(incidentId, limit, offset)

            if (worksitesRequest.errors?.isNotEmpty() == true) {
                throw Exception(
                    NetworkCrisisCleanupApiError.collapseMessages(worksitesRequest.errors!!)
                )
            }

            val worksites = worksitesRequest.results?.map {
                it.asEntity(incidentId, uuidGenerator.uuid(), syncStart)
            } ?: emptyList()
            worksiteDaoPlus.syncExternalWorksites(
                incidentId,
                worksites,
            )

            pagedCount += worksites.size
            offset += limit

            // TODO Check for cancellation and exit gracefully. Save stats as necessary. Be sure to emit CancellationExceptions https://developer.android.com/kotlin/coroutines/coroutines-best-practices#coroutine-cancellable
        }

        val syncSeconds = syncStart.epochSeconds
        return WorksitesSyncStats(
            incidentId,
            syncStart,
            worksitesCount,
            pagedCount,
            SyncAttempt(syncSeconds, syncSeconds, 0)
        )
    }

    // TODO Write tests
    override suspend fun refreshWorksites(incidentId: Long, force: Boolean) =
        withContext(ioDispatcher) {
            if (incidentId == EmptyIncident.id) {
                return@withContext
            }

            // TODO Enforce single process syncing per incident

            isLoading.value = true
            try {
                val syncStart = Clock.System.now()

                var syncStats = querySyncStats(incidentId, syncStart)

                val savedWorksitesCount = worksiteDao.getWorksitesCount(incidentId).first()

                // TODO Sync full should cull invalid worksites. Easiest would be deleting all worksites under an incident.

                val syncFull = force || savedWorksitesCount < syncStats.worksitesCount
                if (syncFull || syncStats.syncAttempt.shouldSyncPassively()) {
                    // TODO Is it possible skip saved worksites and only pull missing worksites or start from syncStats.pagedCount?
                    syncStats = syncWorksitesData(incidentId, syncStart)
                }

                if (syncFull || syncStats.syncAttempt.shouldSyncPassively()) {
                    // TODO Compare last successful with syncStart and determine if extra syncing is necessary
                }

                worksitesSyncStatsDao.upsertStats(syncStats.asEntity())
            } finally {
                isLoading.value = false
            }
        }
}