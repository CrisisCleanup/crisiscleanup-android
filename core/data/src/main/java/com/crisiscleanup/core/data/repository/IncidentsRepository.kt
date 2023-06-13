package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentIdNameType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface IncidentsRepository {
    /**
     * Is loading incidents data
     */
    val isLoading: Flow<Boolean>

    /**
     * Stream of [Incident]s
     */
    val incidents: Flow<List<Incident>>

    suspend fun getIncident(id: Long, loadFormFields: Boolean = false): Incident?
    suspend fun getIncidents(startAt: Instant): List<Incident>

    fun streamIncident(id: Long): Flow<Incident?>

    suspend fun pullIncidents()

    suspend fun pullIncident(id: Long)

    suspend fun pullIncidentOrganizations(incidentId: Long, force: Boolean = false)

    suspend fun getIncidentsForDisplay(): List<IncidentIdNameType>

    suspend fun getMatchingIncidents(q: String): List<IncidentIdNameType>
}