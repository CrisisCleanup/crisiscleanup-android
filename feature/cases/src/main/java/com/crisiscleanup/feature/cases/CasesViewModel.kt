package com.crisiscleanup.feature.cases

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    val incidentSelector: IncidentSelector,
    private val appHeaderUiState: AppHeaderUiState,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
) : ViewModel() {
    var isTableView = mutableStateOf(false)
        private set

    fun setContentViewType(isTableView: Boolean) {
        this.isTableView.value = isTableView
    }

    var isLayerView = mutableStateOf(false)
        private set

    fun toggleLayersView() {
        isLayerView.value = !isLayerView.value
    }

    val isLoadingIncidents = incidentsRepository.isLoading

    init {
        incidentSelector.incident.onEach {
            appHeaderUiState.setTitle(it.name)
        }.launchIn(viewModelScope)
    }

    val incidentsData = incidentsRepository.incidents.map { incidents ->
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
        scope = viewModelScope,
        initialValue = IncidentsData.Loading,
        started = SharingStarted.WhileSubscribed(),
    )

    var casesSearchQuery = mutableStateOf("")
        private set

    fun updateCasesSearchQuery(q: String) {
        casesSearchQuery.value = q
    }

    suspend fun selectIncident(incident: Incident) {
        if (incidentsData.value is IncidentsData.Incidents) {
            val incidents = (incidentsData.value as IncidentsData.Incidents).incidents
            val verifiedIncident = incidents.find { it.id == incident.id }
            if (verifiedIncident != null) {
                incidentSelector.setIncident(incident)

                appPreferencesRepository.setSelectedIncident(incident.id)
            }
        }
    }
}

sealed interface IncidentsData {
    object Loading : IncidentsData

    data class Incidents(
        val incidents: List<Incident>,
    )

    object Empty : IncidentsData
}