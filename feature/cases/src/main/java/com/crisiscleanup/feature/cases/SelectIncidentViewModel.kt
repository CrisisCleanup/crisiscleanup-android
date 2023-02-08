package com.crisiscleanup.feature.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.domain.LoadIncidentDataUseCase
import com.crisiscleanup.core.model.data.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectIncidentViewModel @Inject constructor(
    val incidentSelector: IncidentSelector,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    loadIncidentDataUseCase: LoadIncidentDataUseCase,
) : ViewModel() {
    val incidentsData = loadIncidentDataUseCase()

    fun selectIncident(incident: Incident) {
        viewModelScope.launch {
            (incidentsData.first() as? IncidentsData.Incidents)?.let { data ->
                val incidents = data.incidents
                incidents.find { it.id == incident.id }?.let { matchingIncident ->
                    incidentSelector.setIncident(matchingIncident)

                    appPreferencesRepository.setSelectedIncident(matchingIncident.id)
                }
            }
        }
    }
}