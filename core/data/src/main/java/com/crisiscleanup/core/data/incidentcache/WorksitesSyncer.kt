package com.crisiscleanup.core.data.incidentcache

import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface WorksitesSyncer {
    val dataPullStats: Flow<IncidentDataPullStats>

    suspend fun networkWorksitesCount(
        incidentId: Long,
        updatedAfter: Instant? = null,
    ): Int

    suspend fun sync(
        incidentId: Long,
        syncStats: IncidentDataSyncStats,
    )
}
