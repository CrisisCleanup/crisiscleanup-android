package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import kotlinx.coroutines.flow.Flow

interface WorksitesRepository {
    /**
     * Is loading incidents data
     */
    val isLoading: Flow<Boolean>

    /**
     * Stream of worksite data for map rendering
     */
    suspend fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeMin: Double,
        latitudeMax: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int,
        offset: Int,
    ): Flow<List<WorksiteMapMark>>

    /**
     * Stream of an incident's [Worksite]s
     */
    fun getWorksites(incidentId: Long, limit: Int, offset: Int): Flow<List<Worksite>>

    suspend fun refreshWorksites(incidentId: Long, force: Boolean)
}