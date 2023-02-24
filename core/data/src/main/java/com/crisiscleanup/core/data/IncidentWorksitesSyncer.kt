package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.util.IncidentWorksitesDataPullStats
import com.crisiscleanup.core.data.util.WorksitesDataPullStatsUpdater
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.model.data.WorksitesSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.tryThrowException
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import javax.inject.Inject

interface WorksitesSyncer {
    suspend fun networkWorksitesCount(
        incidentId: Long,
        updatedAfter: Instant? = null,
        throwOnEmpty: Boolean = false,
    ): Int

    suspend fun sync(
        incidentId: Long,
        syncStats: WorksitesSyncStats,
    )
}

// TODO Write tests

class IncidentWorksitesSyncer @Inject constructor(
    private val worksiteNetworkDataSource: CrisisCleanupNetworkDataSource,
    private val networkDataCache: WorksitesNetworkDataCache,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksitesSyncStatsDao: WorksitesSyncStatsDao,
    private val authEventManager: AuthEventManager,
    memoryStats: AppMemoryStats,
    private val appVersionProvider: AppVersionProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : WorksitesSyncer {
    val dataPullStats = MutableStateFlow(IncidentWorksitesDataPullStats())

    // TODO Defer to provider instead. So amount can vary according to (WifiManager) signal level or equivalent. Must track request timeouts and give feedback or adjust.

    private val allWorksitesMemoryThreshold = 100

    private val isCapableDevice = memoryStats.availableMemory >= allWorksitesMemoryThreshold

    override suspend fun networkWorksitesCount(
        incidentId: Long,
        updatedAfter: Instant?,
        throwOnEmpty: Boolean,
    ): Int {
        val worksitesCountResult =
            worksiteNetworkDataSource.getWorksitesCount(incidentId, updatedAfter)
        tryThrowException(authEventManager, worksitesCountResult.errors)

        val count = worksitesCountResult.count ?: 0
        if (throwOnEmpty && count == 0) {
            throw Exception("Backend is reporting no worksites for incident $incidentId")
        }

        return count
    }

    override suspend fun sync(
        incidentId: Long,
        syncStats: WorksitesSyncStats,
    ) {
        val statsUpdater = WorksitesDataPullStatsUpdater(
            updatePullStats = { stats -> dataPullStats.value = stats }
        ).also {
            it.beginPull(incidentId)
        }
        try {
            saveWorksitesData(incidentId, syncStats, statsUpdater)
        } finally {
            statsUpdater.endPull()
        }
    }

    private suspend fun saveWorksitesData(
        incidentId: Long,
        syncStats: WorksitesSyncStats,
        statsUpdater: WorksitesDataPullStatsUpdater,
    ) = coroutineScope {
        val isDeltaPull = syncStats.pagedCount >= syncStats.worksitesCount
        val updatedAfter: Instant?
        val syncCount: Int
        if (isDeltaPull) {
            updatedAfter = Instant.fromEpochSeconds(syncStats.syncAttempt.successfulSeconds)
            syncCount = networkWorksitesCount(incidentId, updatedAfter, true)
        } else {
            updatedAfter = null
            syncCount = syncStats.worksitesCount
        }
        if (syncCount <= 0) {
            return@coroutineScope
        }

        statsUpdater.updateWorksitesCount(syncCount)

        statsUpdater.setPagingRequest()

        var networkPullPage = 0
        var requestingCount = 0
        val pageCount =
            if (isCapableDevice) 10000 else 3000
        try {
            while (requestingCount < syncCount) {
                networkDataCache.saveWorksitesShort(
                    incidentId,
                    pageCount,
                    networkPullPage,
                    syncCount,
                    updatedAfter
                )
                networkPullPage++
                requestingCount += pageCount

                ensureActive()

                val requestedCount = kotlin.math.min(requestingCount, syncCount)
                statsUpdater.updateRequestedCount(requestedCount)
            }
        } catch (e: Exception) {
            if (e is InterruptedException) {
                throw e
            }

            logger.logException(e)
        }

        var startSyncRequestTime: Instant? = null
        var dbSaveCount = 0
        var deleteCacheFiles = false
        for (dbSavePage in 0 until networkPullPage) {
            val cachedData = networkDataCache.loadWorksitesShort(
                incidentId,
                dbSavePage,
                syncCount,
            ) ?: break

            if (startSyncRequestTime == null) {
                startSyncRequestTime = cachedData.requestTime
            }

            // TODO Deltas must account for deleted/reassigned as well

            val saveData = syncStats.pagedCount < dbSaveCount + pageCount || isDeltaPull
            if (saveData) {
                val worksites = cachedData.worksites.map { it.asEntity(incidentId) }
                val workTypes =
                    cachedData.worksites.map { it.workTypes.map(NetworkWorksiteFull.WorkTypeShort::asEntity) }
                saveToDb(
                    incidentId,
                    worksites,
                    workTypes,
                    cachedData.requestTime,
                    statsUpdater,
                )
            } else {
                statsUpdater.addSavedCount(pageCount)
            }

            dbSaveCount += pageCount
            val isSyncEnd = dbSaveCount >= syncCount

            if (saveData) {
                if (isSyncEnd) {
                    worksitesSyncStatsDao.updateStatsSuccessful(
                        incidentId,
                        syncStats.syncStart,
                        syncStats.worksitesCount,
                        startSyncRequestTime,
                        startSyncRequestTime,
                        0,
                        appVersionProvider.versionCode,
                    )
                } else if (!isDeltaPull) {
                    worksitesSyncStatsDao.updateStatsPaged(
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
                networkDataCache.deleteWorksitesShort(incidentId, deleteCachePage)
            }
        }
    }

    private suspend fun saveToDb(
        incidentId: Long,
        worksites: List<WorksiteEntity>,
        workTypes: List<List<WorkTypeEntity>>,
        syncStart: Instant,
        statsUpdater: WorksitesDataPullStatsUpdater,
    ): Int = coroutineScope {
        var offset = 0
        // TODO Make configurable. Depends on the capabilities and/or OS version of the device as well.
        val dbOperationLimit = 500
        val limit = dbOperationLimit.coerceAtLeast(100)
        var pagedCount = 0
        while (offset < worksites.size) {
            val offsetEnd = Integer.min(offset + limit, worksites.size)
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
}
