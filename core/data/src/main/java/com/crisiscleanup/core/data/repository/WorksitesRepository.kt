package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.model.data.WorksitesSyncStats
import kotlinx.coroutines.flow.Flow

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

    suspend fun refreshWorksites(incidentId: Long, force: Boolean)

    fun getWorksitesSyncStats(incidentId: Long): WorksitesSyncStats?
}