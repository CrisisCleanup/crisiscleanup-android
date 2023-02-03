package com.crisiscleanup.feature.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.domain.LoadIncidentDataUseCase
import com.crisiscleanup.core.model.data.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectIncidentViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    val incidentSelector: IncidentSelector,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
) : ViewModel() {
    val incidentsData = LoadIncidentDataUseCase(
        viewModelScope,
        incidentsRepository,
        incidentSelector,
        appPreferencesRepository,
    )()

    fun selectIncident(incident: Incident) {
        (incidentsData.value as? IncidentsData.Incidents)?.let {
            viewModelScope.launch {
                val incidents = it.incidents
                incidents.find { it.id == incident.id }?.let { matchingIncident ->
                    incidentSelector.setIncident(matchingIncident)

                    appPreferencesRepository.setSelectedIncident(matchingIncident.id)
                }
            }
        }
    }
}