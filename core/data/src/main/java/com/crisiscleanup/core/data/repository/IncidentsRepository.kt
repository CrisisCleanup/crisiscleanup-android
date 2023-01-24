package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentsRepository {
    /**
     * Stream of [Incident]s
     */
    val incidents: Flow<List<Incident>>
}