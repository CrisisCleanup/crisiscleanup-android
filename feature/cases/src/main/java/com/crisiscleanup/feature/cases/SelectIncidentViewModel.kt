package com.crisiscleanup.feature.cases

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.domain.LoadIncidentDataUseCase
import com.crisiscleanup.core.model.data.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectIncidentViewModel @Inject constructor(
    val incidentSelector: IncidentSelector,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    loadIncidentDataUseCase: LoadIncidentDataUseCase,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) : ViewModel() {
    val incidentsData = loadIncidentDataUseCase()

    fun selectIncident(incident: Incident) {
        coroutineScope.launch {
            (incidentsData.first() as? IncidentsData.Incidents)?.let { data ->
                data.incidents.find { it.id == incident.id }?.let { matchingIncident ->
                    appPreferencesRepository.setSelectedIncident(matchingIncident.id)

                    incidentSelector.setIncident(matchingIncident)
                }
            }
        }
    }
}