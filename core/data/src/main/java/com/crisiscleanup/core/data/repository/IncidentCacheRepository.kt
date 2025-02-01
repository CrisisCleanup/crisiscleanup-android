package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.asExternalModel
import com.crisiscleanup.core.database.dao.IncidentWorksitesSyncStatDao
import com.crisiscleanup.core.datastore.IncidentCachePreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentWorksitesSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

interface IncidentCacheRepository {
    val syncingIncidentsIds: StateFlow<Set<Long>>
    val dataPullStats: Flow<IncidentDataPullStats>

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
    private val cachePreferences: IncidentCachePreferencesDataSource,
    private val appVersionProvider: AppVersionProvider,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    @Logger(CrisisCleanupLoggers.Sync) private val logger: AppLogger,
) : IncidentCacheRepository {
    override val syncingIncidentsIds = MutableStateFlow<Set<Long>>(emptySet())
    private val syncingIds = mutableSetOf<Long>()

    override val dataPullStats = MutableStateFlow(IncidentDataPullStats())

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

            val preferences = cachePreferences.preferences.first()
            if (preferences.isPaused) {
                return@coroutineScope true
            }

            val syncStatsEntity = worksitesSyncStatsDao.getSyncStats(incidentId)
            val boundedParameters = IncidentWorksitesSyncStats.SyncBoundedParameters(
                latitude = preferences.regionLatitude,
                longitude = preferences.regionLongitude,
                radius = preferences.regionRadiusMiles,
            )
            val syncStats = syncStatsEntity?.asExternalModel(logger)?.copy(
                boundedParameters = boundedParameters,
            ) ?: IncidentWorksitesSyncStats.startingStats(
                incidentId,
                boundedParameters = boundedParameters,
                appBuildVersionCode = appVersionProvider.versionCode,
            )
            if (syncStatsEntity == null) {
                worksitesSyncStatsDao.insertSyncStats(syncStats.asEntity(logger))
            }

            // TODO Update sync stats gradually
            if (preferences.isRegionBounded) {
                // TODO Sync bounded
            } else {
                // TODO Sync unbounded
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

    override suspend fun resetIncidentSyncStats(incidentId: Long) {
        worksitesSyncStatsDao.deleteSyncStats(incidentId)
    }
}
