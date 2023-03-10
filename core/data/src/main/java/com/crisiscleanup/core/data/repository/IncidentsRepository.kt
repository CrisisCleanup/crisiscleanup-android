package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentsRepository {
    /**
     * Is loading incidents data
     */
    val isLoading: Flow<Boolean>

    /**
     * Stream of [Incident]s
     */
    val incidents: Flow<List<Incident>>

    suspend fun getIncident(id: Long): Incident?

    suspend fun pullIncidents()

    suspend fun pullIncident(id: Long)
}