package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.LocalWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.model.data.WorksitesSyncStats
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface WorksitesRepository {
    /**
     * Is loading incidents data
     */
    val isLoading: Flow<Boolean>

    /**
     * Stream of worksite data for map rendering
     */
    suspend fun streamWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int,
        offset: Int,
    ): Flow<List<WorksiteMapMark>>

    /**
     * Stream of an incident's [Worksite]s
     */
    fun streamWorksites(incidentId: Long, limit: Int, offset: Int): Flow<List<Worksite>>

    fun streamIncidentWorksitesCount(id: Long): Flow<Int>

    fun streamLocalWorksite(worksiteId: Long): Flow<LocalWorksite?>

    fun getWorksitesMapVisual(incidentId: Long, limit: Int, offset: Int): List<WorksiteMapMark>

    fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
        limit: Int,
        offset: Int
    ): List<WorksiteMapMark>

    fun getWorksitesCount(incidentId: Long): Int

    fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
    ): Int

    suspend fun refreshWorksites(
        incidentId: Long,
        forceQueryDeltas: Boolean = false,
        forceRefreshAll: Boolean = false,
    )

    fun getWorksitesSyncStats(incidentId: Long): WorksitesSyncStats?

    suspend fun syncWorksite(
        incidentId: Long,
        worksiteNetworkId: Long,
    ): Boolean

    suspend fun syncNetworkWorksite(
        incidentId: Long,
        worksite: NetworkWorksiteFull,
        syncedAt: Instant = Clock.System.now(),
    ): Boolean

    suspend fun getLocalId(incidentId: Long, networkWorksiteId: Long): Long
}