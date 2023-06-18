package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.WorksitesFullSyncer
import com.crisiscleanup.core.data.WorksitesSyncer
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.util.IncidentDataPullReporter
import com.crisiscleanup.core.database.dao.RecentWorksiteDao
import com.crisiscleanup.core.database.dao.WorkTypeTransferRequestDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.database.model.PopulatedRecentWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.RecentWorksiteEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.asSummaryEntity
import com.crisiscleanup.core.database.model.asWorksiteSyncStatsEntity
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

// TODO Clear sync stats on logout? Or is it more efficient to keep? Are there differences in data when different accounts request data?

@Singleton
class OfflineFirstWorksitesRepository @Inject constructor(
    private val worksitesSyncer: WorksitesSyncer,
    private val worksitesFullSyncer: WorksitesFullSyncer,
    private val worksiteSyncStatDao: WorksiteSyncStatDao,
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    accountDataRepository: AccountDataRepository,
    private val languageTranslationsRepository: LanguageTranslationsRepository,
    private val recentWorksiteDao: RecentWorksiteDao,
    private val dataSource: CrisisCleanupNetworkDataSource,
    private val writeApi: CrisisCleanupWriteApi,
    private val workTypeTransferRequestDaoPlus: WorkTypeTransferRequestDaoPlus,
    private val appVersionProvider: AppVersionProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : WorksitesRepository, IncidentDataPullReporter {
    override val isLoading = MutableStateFlow(false)

    override val syncWorksitesFullIncidentId = MutableStateFlow(EmptyIncident.id)

    override val incidentDataPullStats = worksitesSyncer.dataPullStats

    override fun streamWorksites(incidentId: Long, limit: Int, offset: Int) =
        worksiteDao.streamWorksites(incidentId, limit, offset)
            .map { it.map(PopulatedWorksite::asExternalModel) }

    override fun streamIncidentWorksitesCount(id: Long) = worksiteDao.streamWorksitesCount(id)

    private val orgId = accountDataRepository.accountData.map { it.org.id }

    override fun streamLocalWorksite(worksiteId: Long) =
        worksiteDao.streamLocalWorksite(worksiteId).map {
            it?.asExternalModel(
                orgId.first(),
                languageTranslationsRepository,
            )
        }

    override suspend fun streamWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int,
        offset: Int,
    ) = withContext(ioDispatcher) {
        return@withContext worksiteDaoPlus.streamWorksitesMapVisual(
            incidentId,
            latitudeSouth,
            latitudeNorth,
            longitudeLeft,
            longitudeRight,
            limit,
            offset,
        )
            .map { it.map(PopulatedWorksiteMapVisual::asExternalModel) }
    }

    override fun streamRecentWorksites(incidentId: Long) =
        recentWorksiteDao.streamRecentWorksites(incidentId)
            .map { it.map(PopulatedRecentWorksite::asSummaryEntity) }

    override fun getWorksitesMapVisual(
        incidentId: Long,
        limit: Int,
        offset: Int
    ) = worksiteDao.getWorksitesMapVisual(incidentId, limit, offset)
        .map(PopulatedWorksiteMapVisual::asExternalModel)

    override fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
        limit: Int,
        offset: Int
    ) = worksiteDao.getWorksitesMapVisual(
        incidentId,
        latitudeSouth,
        latitudeNorth,
        longitudeWest,
        longitudeEast,
        limit,
        offset,
    )
        .map(PopulatedWorksiteMapVisual::asExternalModel)

    override fun getWorksitesCount(incidentId: Long) = worksiteDao.getWorksitesCount(incidentId)

    override fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double
    ) = worksiteDaoPlus.getWorksitesCount(
        incidentId,
        latitudeSouth,
        latitudeNorth,
        longitudeLeft,
        longitudeRight
    )

    override fun getLocalId(networkWorksiteId: Long) = worksiteDao.getWorksiteId(networkWorksiteId)

    override fun getWorksiteSyncStats(incidentId: Long) =
        worksiteSyncStatDao.getSyncStats(incidentId)?.asExternalModel()

    private suspend fun queryUpdatedSyncStats(
        incidentId: Long,
        reset: Boolean,
    ): IncidentDataSyncStats {
        if (!reset) {
            val syncStatsQuery = worksiteSyncStatDao.getSyncStats(incidentId)
            syncStatsQuery?.let {
                val syncStats = it.asExternalModel()
                if (!syncStats.isDataVersionOutdated) {
                    return syncStats
                }
            }
        }

        val syncStart = Clock.System.now()
        val worksitesCount =
            worksitesSyncer.networkWorksitesCount(incidentId, Instant.fromEpochSeconds(0))
        val syncStats = IncidentDataSyncStats(
            incidentId,
            syncStart,
            worksitesCount,
            0,
            // TODO Preserve previous attempt metrics (if used)
            SyncAttempt(0, 0, 0),
            appVersionProvider.versionCode,
        )
        worksiteSyncStatDao.upsertStats(syncStats.asWorksiteSyncStatsEntity())
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
                savedWorksitesCount < syncStats.dataCount ||
                forceQueryDeltas
            ) {
                worksitesSyncer.sync(incidentId, syncStats)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Updating sync stats here (or in finally) could overwrite "concurrent" sync that previously started. Think it through before updating sync attempt.

            logger.logException(e)
        } finally {
            isLoading.value = false
        }
    }

    override suspend fun syncWorksitesFull(incidentId: Long): Boolean = coroutineScope {
        if (incidentId == EmptyIncident.id) {
            return@coroutineScope true
        }

        syncWorksitesFullIncidentId.value = incidentId
        try {
            worksitesFullSyncer.sync(incidentId)
            return@coroutineScope true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.logException(e)
        } finally {
            syncWorksitesFullIncidentId.value = EmptyIncident.id
        }
        return@coroutineScope false
    }

    override suspend fun syncNetworkWorksite(
        worksite: NetworkWorksiteFull,
        syncedAt: Instant,
    ): Boolean {
        val entities = worksite.asEntities()
        return worksiteDaoPlus.syncNetworkWorksite(entities, syncedAt)
    }

    override suspend fun pullWorkTypeRequests(networkWorksiteId: Long) {
        try {
            val workTypeRequests = dataSource.getWorkTypeRequests(networkWorksiteId)
            if (workTypeRequests.isNotEmpty()) {
                val worksiteId = worksiteDao.getWorksiteId(networkWorksiteId)
                val entities = workTypeRequests.map { it.asEntity(worksiteId) }
                workTypeTransferRequestDaoPlus.syncUpsert(entities)
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun setRecentWorksite(
        incidentId: Long,
        worksiteId: Long,
        viewStart: Instant,
    ) {
        if (worksiteId <= 0) {
            return
        }

        recentWorksiteDao.upsert(
            RecentWorksiteEntity(
                id = worksiteId,
                incidentId = incidentId,
                viewedAt = viewStart,
            )
        )
    }

    override fun getUnsyncedCounts(worksiteId: Long) =
        worksiteDaoPlus.getUnsyncedChangeCount(worksiteId)

    override suspend fun shareWorksite(
        worksiteId: Long,
        emails: List<String>,
        phoneNumbers: List<String>,
        shareMessage: String,
        noClaimReason: String?
    ): Boolean {
        try {
            writeApi.shareWorksite(
                worksiteId,
                emails,
                phoneNumbers,
                shareMessage,
                noClaimReason,
            )

            return true
        } catch (e: Exception) {
            // TODO Show dialog
            logger.logException(e)
        }
        return false
    }
}
