package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.data.model.asExternalModel
import com.crisiscleanup.core.database.dao.IncidentWorksitesSyncStatsDao
import com.crisiscleanup.core.datastore.IncidentCachePreferencesDataSource
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.model.data.IncidentWorksitesSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentCacheRepository {
    val dataPullStats: Flow<IncidentDataPullStats>

    fun streamSyncStats(incidentId: Long): Flow<IncidentWorksitesSyncStats?>

    suspend fun sync(
        incidentId: Long,
        syncStats: IncidentDataSyncStats,
    )

    suspend fun resetIncidentSyncStats(incidentId: Long)
}

@Singleton
class IncidentWorksitesCacheRepository @Inject constructor(
    private val worksitesSyncStatsDao: IncidentWorksitesSyncStatsDao,
    private val cachePreferences: IncidentCachePreferencesDataSource,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
) : IncidentCacheRepository {
    private val syncingIncidents = mutableSetOf<Long>()

    override val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    override fun streamSyncStats(incidentId: Long) =
        worksitesSyncStatsDao.streamWorksitesSyncStats(incidentId)
            .map { it?.asExternalModel() }

    override suspend fun sync(incidentId: Long, syncStats: IncidentDataSyncStats) {
        // TODO
    }

    override suspend fun resetIncidentSyncStats(incidentId: Long) {
        worksitesSyncStatsDao.deleteSyncStats(incidentId)
    }
}
