package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Worksites
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.incidentcache.SyncCacheDeviceInspector
import com.crisiscleanup.core.data.incidentcache.WorksitesSyncer
import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.util.IncidentDataPullStatsUpdater
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import javax.inject.Inject

// TODO Test coverage

class IncidentWorksitesSyncer @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val networkDataCache: WorksitesNetworkDataCache,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksiteSyncStatDao: WorksiteSyncStatDao,
    private val deviceInspector: SyncCacheDeviceInspector,
    private val appVersionProvider: AppVersionProvider,
    @Logger(Worksites) private val logger: AppLogger,
) : WorksitesSyncer {
    override val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    override suspend fun networkWorksitesCount(incidentId: Long, updatedAfter: Instant?) =
        networkDataSource.getWorksitesCount(incidentId, updatedAfter)

    override suspend fun sync(
        incidentId: Long,
        syncStats: IncidentDataSyncStats,
    ) {
        val statsUpdater = IncidentDataPullStatsUpdater(
            updatePullStats = { stats -> dataPullStats.value = stats },
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
        val pageCount = if (deviceInspector.isLimitedDevice) 3000 else 10000
        try {
            while (requestingCount < syncCount) {
                networkDataCache.saveWorksitesShort(
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

            // TODO Deltas must account for deleted and/or reassigned if not inherently accounted for

            val saveData = syncStats.pagedCount < dbSaveCount + pageCount || isDeltaPull
            if (saveData) {
                with(cachedData.worksites) {
                    val worksites = map { it.asEntity() }
                    val flags = map {
                        it.flags.filter { flag -> flag.invalidatedAt == null }
                            .map(NetworkWorksiteFull.FlagShort::asEntity)
                    }
                    val workTypes = map {
                        it.newestWorkTypes.map(NetworkWorksiteFull.WorkTypeShort::asEntity)
                    }
                    saveToDb(
                        worksites,
                        flags,
                        workTypes,
                        cachedData.requestTime,
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
                    worksiteSyncStatDao.updateStatsSuccessful(
                        incidentId,
                        syncStats.syncStart,
                        syncStats.dataCount,
                        startSyncRequestTime,
                        startSyncRequestTime,
                        0,
                        appVersionProvider.versionCode,
                    )
                } else if (!isDeltaPull) {
                    worksiteSyncStatDao.updateStatsPaged(
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
        worksites: List<WorksiteEntity>,
        flags: List<List<WorksiteFlagEntity>>,
        workTypes: List<List<WorkTypeEntity>>,
        syncStart: Instant,
        statsUpdater: IncidentDataPullStatsUpdater,
    ): Int = coroutineScope {
        var offset = 0
        // TODO Make configurable. Depends on the capabilities and/or OS version of the device as well.
        val dbOperationLimit = 500
        val limit = dbOperationLimit.coerceAtLeast(100)
        var pagedCount = 0
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

            pagedCount += worksiteSubset.size

            offset += limit

            ensureActive()
        }
        return@coroutineScope pagedCount
    }
}
