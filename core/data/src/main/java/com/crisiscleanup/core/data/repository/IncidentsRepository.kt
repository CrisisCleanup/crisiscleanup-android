package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.data.Syncable
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentsRepository : Syncable {
    /**
     * Is loading incidents data
     */
    val isLoading: Flow<Boolean>

    /**
     * Stream of [Incident]s
     */
    val incidents: Flow<List<Incident>>

    suspend fun getIncident(id: Long): Incident?
}