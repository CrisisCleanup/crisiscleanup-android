package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentIdNameType
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface IncidentsRepository {
    val isLoading: Flow<Boolean>

    val isFirstLoad: Flow<Boolean>

    val incidentCount: Long

    /**
     * Stream of [Incident]s
     */
    val incidents: Flow<List<Incident>>

    /**
     * Stream of [Incident]s with active phone numbers
     */
    val hotlineIncidents: Flow<List<Incident>>

    suspend fun getIncident(id: Long, loadFormFields: Boolean = false): Incident?
    suspend fun getIncidents(startAt: Instant): List<Incident>
    suspend fun getIncidentsList(): List<IncidentIdNameType>

    fun streamIncident(id: Long): Flow<Incident?>

    suspend fun pullIncidents(force: Boolean)
    suspend fun pullHotlineIncidents()

    suspend fun pullIncident(id: Long)

    // TODO Not all callers handle errors. Trace and handle properly.
    suspend fun pullIncidentOrganizations(incidentId: Long, force: Boolean = false)

    suspend fun getMatchingIncidents(q: String): List<IncidentIdNameType>
}
