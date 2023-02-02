package com.crisiscleanup.feature.cases

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class IncidentsDataLoader constructor(
    coroutineScope: CoroutineScope,
    incidentsRepository: IncidentsRepository,
    private val incidentSelector: IncidentSelector,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
) : ViewModel() {
    val data = incidentsRepository.incidents.map { incidents ->
        var selectedId = incidentSelector.incidentId.first()
        if (selectedId == EmptyIncident.id) {
            selectedId = appPreferencesRepository.userData.first().selectedIncidentId
        }

        // Update incident data or select first if current incident (ID) not found
        var incident = incidents.find { it.id == selectedId } ?: EmptyIncident
        if (incident == EmptyIncident && incidents.isNotEmpty()) {
            incident = incidents[0]
        }

        incidentSelector.setIncident(incident)

        if (incidentSelector.incidentId.first() == EmptyIncident.id) IncidentsData.Empty
        else IncidentsData.Incidents(incidents)
    }.stateIn(
        scope = coroutineScope,
        initialValue = IncidentsData.Loading,
        started = SharingStarted.WhileSubscribed(),
    )
}

sealed interface IncidentsData {
    object Loading : IncidentsData

    data class Incidents(
        val incidents: List<Incident>,
    )

    object Empty : IncidentsData
}