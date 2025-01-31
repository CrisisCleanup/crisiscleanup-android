package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.datastore.IncidentCachePreferencesDataSource
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentCacheRepository {
    val dataPullStats: Flow<IncidentDataPullStats>

    suspend fun sync(
        incidentId: Long,
        syncStats: IncidentDataSyncStats,
    )
}

@Singleton
class IncidentWorksitesCacheRepository @Inject constructor(
    private val cachePreferences: IncidentCachePreferencesDataSource,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
) : IncidentCacheRepository {
    private val syncingIncidents = mutableSetOf<Long>()

    override val dataPullStats = MutableStateFlow(IncidentDataPullStats())

    override suspend fun sync(incidentId: Long, syncStats: IncidentDataSyncStats) {
        // TODO
    }
}
