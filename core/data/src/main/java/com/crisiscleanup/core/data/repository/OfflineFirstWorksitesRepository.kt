package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentWorksitesSyncer
import com.crisiscleanup.core.data.util.WorksitesDataPullReporter
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import com.crisiscleanup.core.database.model.*
import com.crisiscleanup.core.model.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

// TODO Clear sync stats on logout? Or is it more efficient to keep? Are there differences in data when different accounts request data?

@Singleton
class OfflineFirstWorksitesRepository @Inject constructor(
    private val worksitesSyncer: IncidentWorksitesSyncer,
    private val worksitesSyncStatsDao: WorksitesSyncStatsDao,
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val appVersionProvider: AppVersionProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : WorksitesRepository, WorksitesDataPullReporter {
    override var isLoading = MutableStateFlow(false)
        private set

    override val worksitesDataPullStats = worksitesSyncer.dataPullStats

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

    override fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double
    ): Int = worksiteDaoPlus.getWorksitesCount(
        incidentId,
        latitudeSouth,
        latitudeNorth,
        longitudeLeft,
        longitudeRight
    )

    override fun getWorksitesSyncStats(incidentId: Long): WorksitesSyncStats? =
        worksitesSyncStatsDao.getSyncStats(incidentId).firstOrNull()?.asExternalModel()

    private suspend fun queryUpdatedSyncStats(
        incidentId: Long,
        reset: Boolean,
    ): WorksitesSyncStats {
        if (!reset) {
            val syncStatsQuery = worksitesSyncStatsDao.getSyncStats(incidentId)
            syncStatsQuery.firstOrNull()?.let {
                val syncStats = it.asExternalModel()
                if (!syncStats.isDataVersionOutdated) {
                    return syncStats
                }
            }
        }

        val syncStart = Clock.System.now()
        val worksitesCount =
            worksitesSyncer.networkWorksitesCount(incidentId, Instant.fromEpochSeconds(0), true)
        val syncStats = WorksitesSyncStats(
            incidentId,
            syncStart,
            worksitesCount,
            0,
            // TODO Preserve previous attempt metrics (if used)
            SyncAttempt(0, 0, 0),
            appVersionProvider.versionCode,
        )
        worksitesSyncStatsDao.upsertStats(syncStats.asEntity())
        return syncStats
    }

    // TODO Write tests
    override suspend fun refreshWorksites(
        incidentId: Long,
        forceQueryDeltas: Boolean,
        forceRefreshAll: Boolean,
    ) = coroutineScope {
        if (incidentId == EmptyIncident.id) {
            return@coroutineScope
        }

        // TODO Enforce single process syncing per incident since this may be very long running

        isLoading.value = true

        try {
            val syncStats = queryUpdatedSyncStats(incidentId, forceRefreshAll)
            val savedWorksitesCount = worksiteDao.getWorksitesCount(incidentId)
            if (syncStats.syncAttempt.shouldSyncPassively() ||
                savedWorksitesCount < syncStats.worksitesCount ||
                forceQueryDeltas
            ) {
                worksitesSyncer.sync(incidentId, syncStats)
                // TODO Sync additional if above sync was full
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw (e)
            } else {
                // Updating sync stats here (or in finally) could overwrite "concurrent" sync that previously started. Think it through before updating sync attempt.

                // TODO User feedback?
                logger.logException(e)
            }
        } finally {
            isLoading.value = false
        }
    }
}
