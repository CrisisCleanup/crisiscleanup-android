package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.asExternalModel
import com.crisiscleanup.core.database.dao.IncidentWorksitesSyncStatDao
import com.crisiscleanup.core.datastore.IncidentCachePreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentWorksitesCachePreferences
import com.crisiscleanup.core.model.data.IncidentWorksitesSyncStats
import com.crisiscleanup.core.model.data.IncidentWorksitesSyncStats.SyncStepTimestamps
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

interface IncidentCacheRepository {
    val syncingIncidentsIds: StateFlow<Set<Long>>
    val isSyncingActiveIncident: Flow<Boolean>
    val dataPullStats: Flow<IncidentDataPullStats>

    val cachePreferences: Flow<IncidentWorksitesCachePreferences>

    fun streamSyncStats(incidentId: Long): Flow<IncidentWorksitesSyncStats?>

    suspend fun sync(
        incidentId: Long,
        forceRefreshAll: Boolean = false,
    ): Boolean

    suspend fun resetIncidentSyncStats(incidentId: Long)
}

@Singleton
class IncidentWorksitesCacheRepository @Inject constructor(
    private val worksitesSyncStatsDao: IncidentWorksitesSyncStatDao,
    private val incidentCachePreferences: IncidentCachePreferencesDataSource,
    incidentSelector: IncidentSelector,
    private val appVersionProvider: AppVersionProvider,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    @Logger(CrisisCleanupLoggers.Sync) private val logger: AppLogger,
) : IncidentCacheRepository {

    override val syncingIncidentsIds = MutableStateFlow<Set<Long>>(emptySet())
    private val syncingIds = mutableSetOf<Long>()

    override val isSyncingActiveIncident = combine(
        incidentSelector.incidentId,
        syncingIncidentsIds,
        ::Pair,
    )
        .map { (incidentId, syncingIncidentIds) ->
            syncingIncidentIds.contains(incidentId)
        }

    override val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    override val cachePreferences = incidentCachePreferences.preferences

    override fun streamSyncStats(incidentId: Long) =
        worksitesSyncStatsDao.streamWorksitesSyncStats(incidentId)
            .map { it?.asExternalModel(logger) }

    override suspend fun sync(
        incidentId: Long,
        forceRefreshAll: Boolean,
    ) = coroutineScope {
        if (incidentId == EmptyIncident.id) {
            return@coroutineScope true
        }
        synchronized(syncingIncidentsIds) {
            if (syncingIds.contains(incidentId)) {
                return@coroutineScope true
            }
            syncingIds.add(incidentId)
            syncingIncidentsIds.value = syncingIds.toSet()
        }

        try {
            if (forceRefreshAll) {
                resetIncidentSyncStats(incidentId)
            }

            val preferences = cachePreferences.first()
            if (preferences.isPaused) {
                return@coroutineScope true
            }

            val syncStatsEntity = worksitesSyncStatsDao.getSyncStats(incidentId)
            val boundedRegion = IncidentWorksitesSyncStats.BoundedRegion(
                latitude = preferences.regionLatitude,
                longitude = preferences.regionLongitude,
                radius = preferences.regionRadiusMiles,
            )
            val syncStats = syncStatsEntity?.asExternalModel(logger)?.copy(
                boundedRegion = boundedRegion,
            ) ?: IncidentWorksitesSyncStats(
                incidentId,
                syncSteps = SyncStepTimestamps.relative(),
                boundedRegion,
                Clock.System.now(),
                appVersionProvider.versionCode,
            )
            if (syncStatsEntity == null) {
                worksitesSyncStatsDao.insertSyncStats(syncStats.asEntity(logger))
            }

            if (preferences.isRegionBounded) {
                // TODO Sync bounded. Alert if speed if sufficient for fully bounded.
                syncBoundedIncidentWorksites(syncStats)
            } else {
                // TODO Sync unbounded. Alert if speed is low and should toggle bounded.
                syncIncidentWorksites(syncStats)
            }

            return@coroutineScope true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.logException(e)
        } finally {
            synchronized(syncingIds) {
                syncingIds.remove(incidentId)
                syncingIncidentsIds.value = syncingIds.toSet()
            }
        }

        return@coroutineScope false
    }

    private suspend fun syncBoundedIncidentWorksites(syncStatsInitial: IncidentWorksitesSyncStats) =
        coroutineScope {
            // TODO Update stats during sync
            Thread.sleep(1000)
        }

    private suspend fun syncIncidentWorksites(syncStatsInitial: IncidentWorksitesSyncStats) =
        coroutineScope {
            // TODO Update stats during sync
            Thread.sleep(3000)
        }

    override suspend fun resetIncidentSyncStats(incidentId: Long) {
        worksitesSyncStatsDao.deleteSyncStats(incidentId)
    }
}
