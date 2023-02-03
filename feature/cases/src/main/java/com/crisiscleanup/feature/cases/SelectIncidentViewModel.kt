package com.crisiscleanup.feature.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SelectIncidentViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    val incidentSelector: IncidentSelector,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
) : ViewModel() {
    val incidentsData = IncidentsDataLoader(
        viewModelScope,
        incidentsRepository,
        incidentSelector,
        appPreferencesRepository,
    ).data

    suspend fun selectIncident(incident: Incident) {
        if (incidentsData.value is IncidentsData.Incidents) {
            val incidents = (incidentsData.value as IncidentsData.Incidents).incidents
            incidents.find { it.id == incident.id }?.let {
                incidentSelector.setIncident(incident)

                appPreferencesRepository.setSelectedIncident(incident.id)
            }
        }
    }
}