package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asWorksiteEntity
import com.crisiscleanup.core.data.util.IncidentDataPullStats
import com.crisiscleanup.core.data.util.IncidentDataPullStatsUpdater
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.database.model.IncidentWorksitesSecondarySyncStatsEntity
import com.crisiscleanup.core.database.model.PopulatedIncidentSyncStats
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.asSecondaryWorksiteSyncStatsEntity
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.KeyDynamicValuePair
import com.crisiscleanup.core.network.model.NetworkFlagsFormData
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

interface WorksitesSecondaryDataSyncer {
    val dataPullStats: Flow<IncidentDataPullStats>

    suspend fun sync(
        incidentId: Long,
        syncStats: PopulatedIncidentSyncStats,
    )
}

class IncidentWorksitesSecondaryDataSyncer @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val networkDataCache: WorksitesNetworkDataCache,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksiteSyncStatDao: WorksiteSyncStatDao,
    private val deviceInspector: SyncCacheDeviceInspector,
    private val appVersionProvider: AppVersionProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : WorksitesSecondaryDataSyncer {
    override val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    private suspend fun networkWorksitesCount(incidentId: Long, updatedAfter: Instant?) =
        networkDataSource.getWorksitesCount(incidentId, updatedAfter)

    private suspend fun getCleanSyncStats(incidentId: Long): IncidentDataSyncStats {
        val worksitesCount = networkWorksitesCount(incidentId, null)
        return IncidentDataSyncStats(
            incidentId,
            Clock.System.now(),
            worksitesCount,
            0,
            SyncAttempt(0, 0, 0),
            appVersionProvider.versionCode,
        )
    }

    override suspend fun sync(incidentId: Long, syncStats: PopulatedIncidentSyncStats) {
        syncSecondaryData(incidentId, syncStats.secondaryStats)
    }

    private suspend fun syncSecondaryData(
        incidentId: Long,
        secondarySyncStats: IncidentWorksitesSecondarySyncStatsEntity?,
    ) {
        val statsUpdater = IncidentDataPullStatsUpdater(
            updatePullStats = { stats -> dataPullStats.value = stats },
        ).also {
            it.beginPull(incidentId)
        }
        try {
            var syncStats = secondarySyncStats?.asExternalModel() ?: getCleanSyncStats(incidentId)
            if (syncStats.isDataVersionOutdated) {
                syncStats = getCleanSyncStats(incidentId)
            }
            saveSecondaryWorksitesData(incidentId, syncStats, statsUpdater)
        } finally {
            statsUpdater.endPull()
        }
    }

    private suspend fun saveSecondaryWorksitesData(
        incidentId: Long,
        syncStats: IncidentDataSyncStats,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        val isDeltaPull = syncStats.isDeltaPull
        val updatedAfter: Instant?
        val syncCount: Int
        if (isDeltaPull) {
            updatedAfter = Instant.fromEpochSeconds(syncStats.syncAttempt.successfulSeconds)
            syncCount = networkWorksitesCount(incidentId, updatedAfter)
        } else {
            updatedAfter = null
            syncCount = syncStats.dataCount
        }
        if (syncCount <= 0) {
            return@coroutineScope
        }

        statsUpdater.updateDataCount(syncCount)

        statsUpdater.setPagingRequest()

        var networkPullPage = 0
        var requestingCount = 0
        // TODO Review if these page counts are optimal for secondary data
        val pageCount = if (deviceInspector.isLimitedDevice) 3000 else 5000
        try {
            while (requestingCount < syncCount) {
                networkDataCache.saveWorksitesSecondaryData(
                    incidentId,
                    pageCount,
                    networkPullPage,
                    syncCount,
                    updatedAfter,
                )
                networkPullPage++
                requestingCount += pageCount

                ensureActive()

                val requestedCount = requestingCount.coerceAtMost(syncCount)
                statsUpdater.updateRequestedCount(requestedCount)
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }

            logger.logException(e)
        }

        worksiteSyncStatDao.upsertSecondaryStats(syncStats.asSecondaryWorksiteSyncStatsEntity())

        var startSyncRequestTime: Instant? = null
        var dbSaveCount = 0
        var deleteCacheFiles = false
        for (dbSavePage in 0 until networkPullPage) {
            val cachedData = networkDataCache.loadWorksitesSecondaryData(
                incidentId,
                dbSavePage,
                syncCount,
            ) ?: break

            if (startSyncRequestTime == null) {
                startSyncRequestTime = cachedData.requestTime
            }

            // TODO Deltas must account for deleted and/or reassigned if not inherently accounted for

            val saveData = syncStats.pagedCount < dbSaveCount + pageCount || isDeltaPull
            if (saveData) {
                with(cachedData.secondaryData) {
                    val worksitesIds = map(NetworkFlagsFormData::id)
                    val formData = map {
                        it.formData.map(KeyDynamicValuePair::asWorksiteEntity)
                    }
                    val reportedBys = map(NetworkFlagsFormData::reportedBy)
                    saveToDb(
                        worksitesIds,
                        formData,
                        reportedBys,
                        statsUpdater,
                    )
                }
            } else {
                statsUpdater.addSavedCount(pageCount)
            }

            dbSaveCount += pageCount
            val isSyncEnd = dbSaveCount >= syncCount

            if (saveData) {
                if (isSyncEnd) {
                    worksiteSyncStatDao.updateSecondaryStatsSuccessful(
                        incidentId,
                        syncStats.syncStart,
                        syncStats.dataCount,
                        startSyncRequestTime,
                        startSyncRequestTime,
                        0,
                        appVersionProvider.versionCode,
                    )
                } else if (!isDeltaPull) {
                    worksiteSyncStatDao.updateSecondaryStatsPaged(
                        incidentId,
                        syncStats.syncStart,
                        dbSaveCount,
                    )
                }
            }

            if (isSyncEnd) {
                deleteCacheFiles = true
                break
            }
        }

        if (deleteCacheFiles) {
            for (deleteCachePage in 0 until networkPullPage) {
                networkDataCache.deleteWorksitesSecondaryData(incidentId, deleteCachePage)
            }
        }
    }

    private suspend fun saveToDb(
        worksiteIds: List<Long>,
        formData: List<List<WorksiteFormDataEntity>>,
        reportedBys: List<Long?>,
        statsUpdater: IncidentDataPullStatsUpdater,
    ): Int = coroutineScope {
        var offset = 0
        // TODO Make configurable. Depends on the capabilities and/or OS version of the device as well.
        val dbOperationLimit = 500
        val limit = dbOperationLimit.coerceAtLeast(100)
        var pagedCount = 0
        while (offset < worksiteIds.size) {
            val offsetEnd = (offset + limit).coerceAtMost(worksiteIds.size)
            val worksiteIdsSubset = worksiteIds.slice(offset until offsetEnd)
            val formDataSubset = formData.slice(offset until offsetEnd)
            val reportedBysSubset = reportedBys.slice(offset until offsetEnd)
            // Flags should have been saved by IncidentWorksitesSyncer
            worksiteDaoPlus.syncAdditionalData(
                worksiteIdsSubset,
                formDataSubset,
                reportedBysSubset,
            )

            statsUpdater.addSavedCount(worksiteIdsSubset.size)

            pagedCount += worksiteIdsSubset.size

            offset += limit

            ensureActive()
        }
        return@coroutineScope pagedCount
    }
}
