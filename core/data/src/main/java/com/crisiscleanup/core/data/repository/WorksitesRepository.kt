package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Worksite
import kotlinx.coroutines.flow.Flow

interface WorksitesRepository {
    /**
     * Is loading incidents data
     */
    val isLoading: Flow<Boolean>

    /**
     * Stream of an incident's [Worksite]s
     */
    fun getWorksites(incidentId: Long): Flow<List<Worksite>>

    suspend fun refreshWorksites(incidentId: Long, force: Boolean)
}