package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.data.Syncable
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentsRepository : Syncable {
    /**
     * Stream of [Incident]s
     */
    val incidents: Flow<List<Incident>>

    /**
     * Syncs incidents data (from network)
     */
    suspend fun sync(force: Boolean)
}