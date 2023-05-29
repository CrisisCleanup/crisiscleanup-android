package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.util.IncidentDataPullStats
import com.crisiscleanup.core.data.util.IncidentDataPullStatsUpdater
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.database.model.IncidentWorksitesFullSyncStatsEntity
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.math.abs

interface WorksitesSyncer {
    suspend fun networkWorksitesCount(
        incidentId: Long,
        updatedAfter: Instant? = null,
    ): Int

    suspend fun syncShort(
        incidentId: Long,
        syncStats: IncidentDataSyncStats,
    )

    suspend fun syncFull(incidentId: Long)
}

// TODO Test coverage

class IncidentWorksitesSyncer @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val networkDataCache: WorksitesNetworkDataCache,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksiteSyncStatDao: WorksiteSyncStatDao,
    memoryStats: AppMemoryStats,
    private val appVersionProvider: AppVersionProvider,
    private val locationProvider: LocationProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : WorksitesSyncer {
    val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    // TODO Defer to provider instead. So amount can vary according to (WifiManager) signal level or equivalent. Must track request timeouts and give feedback or adjust.

    private val allWorksitesMemoryThreshold = 100

    private val isCapableDevice = memoryStats.availableMemory >= allWorksitesMemoryThreshold

    override suspend fun networkWorksitesCount(incidentId: Long, updatedAfter: Instant?) =
        networkDataSource.getWorksitesCount(incidentId, updatedAfter)

    override suspend fun syncShort(
        incidentId: Long,
        syncStats: IncidentDataSyncStats,
    ) {
        val statsUpdater = IncidentDataPullStatsUpdater(
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

    override suspend fun syncFull(incidentId: Long) {
        worksiteSyncStatDao.getIncidentSyncStats(incidentId)?.let { syncStats ->
            if (syncStats.isShortSynced()) {
                val fullStats = syncStats.fullStats ?: IncidentWorksitesFullSyncStatsEntity(
                    syncStats.entity.incidentId,
                    syncedAt = null,
                    isMyLocationCentered = false,
                    latitude = 999.0,
                    longitude = 999.0,
                    radius = 0.0,
                )
                saveWorksitesFull(syncStats.entity.targetCount, fullStats)
            }
        }
    }

    private suspend fun saveWorksitesData(
        incidentId: Long,
        syncStats: IncidentDataSyncStats,
        statsUpdater: IncidentDataPullStatsUpdater,
    ) = coroutineScope {
        val isDeltaPull = syncStats.pagedCount >= syncStats.dataCount
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

                val requestedCount = requestingCount.coerceAtMost(syncCount)
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
                        incidentId,
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
        incidentId: Long,
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
                incidentId,
                worksiteSubset,
                workTypeSubset,
                syncStart,
            )

            val flagSubset = flags.slice(offset until offsetEnd)
            worksiteDaoPlus.syncShortFlags(
                incidentId,
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

    private val fullSyncPageCount = 40

    private suspend fun saveWorksitesFull(
        initialSyncCount: Int,
        syncStats: IncidentWorksitesFullSyncStatsEntity
    ) = coroutineScope {
        val incidentId = syncStats.incidentId

        var latitude = syncStats.latitude
        var longitude = syncStats.longitude
        if (syncStats.isMyLocationCentered) {
            locationProvider.getLocation()?.let { location ->
                latitude = location.first
                longitude = location.second
            }
        }

        val searchRadius = syncStats.radius
        val locationChangeLength = searchRadius * 0.1
        val hasLocation = abs(latitude) <= 90 && abs(longitude) <= 180 && searchRadius > 0
        val hasLocationChange = hasLocation &&
                abs(latitude - syncStats.latitude) * 111 > locationChangeLength &&
                abs(longitude - syncStats.longitude) * 111 > locationChangeLength

        var isSynced = false
        var pullAll = syncStats.syncedAt == null
        // TODO Adjust according to strength of network connection in real time
        val syncPageCount = fullSyncPageCount * (if (isCapableDevice) 2 else 1)
        val largeIncidentWorksitesCount = syncPageCount * 15
        val worksiteCount = networkWorksitesCount(incidentId)
        val syncStartedAt = Clock.System.now()

        ensureActive()

        // TODO Publish progress for visualization elsewhere

        if (!pullAll) {
            val approximateDeltaCount = worksiteCount - initialSyncCount
            if (approximateDeltaCount < syncPageCount) {
                // TODO Delta pull IDs then all. Single pull.
            } else {
                if (hasLocationChange) {
                    pullAll = true
                } else {
                    // TODO Delta pull IDs by date ranges then all. Multi sync. Cancelable.
                }
            }
        }

        if (pullAll) {
            if (worksiteCount < largeIncidentWorksitesCount) {
                var pagedCount = 0
                while (pagedCount < worksiteCount) {
                    val worksites = networkDataSource.getWorksitesCoreData(
                        incidentId,
                        syncPageCount,
                        pagedCount,
                    )
                    if (worksites?.isNotEmpty() == true) {
                        val entities = worksites.map { it.asEntities() }
                        worksiteDaoPlus.syncWorksites(incidentId, entities, syncStartedAt)
                        pagedCount += worksites.size

                        ensureActive()
                    } else {
                        break
                    }
                }
                isSynced = pagedCount >= worksiteCount
            } else {
                if (hasLocation) {
                    // TODO Query all worksite IDs in the location account for limit
                    //      Greater than limit should page by boundaries
                    //       Cancelable.

                } else {
                    // TODO Signal user to set worksite center and radius
                }
            }
        }

        if (isSynced) {
            worksiteSyncStatDao.upsert(syncStats.copy(syncedAt = syncStartedAt))
        }
    }
}
