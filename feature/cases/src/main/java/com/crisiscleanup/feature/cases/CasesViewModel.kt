package com.crisiscleanup.feature.cases

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    private val incidentSelector: IncidentSelector,
    private val appHeaderUiState: AppHeaderUiState,
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

    val selectedIncidentId: Long
        get() = incidentSelector.incident.id

    val isLoadingIncidents = incidentsRepository.isLoading

    private fun updateAppTitle() = appHeaderUiState.setTitle(incidentSelector.incident.name)

    val incidentsData = incidentsRepository.incidents.map { incidents ->
        // TODO Save and load from prefs
        var selectedId = incidentSelector.incidentId

        // Update incident data or select first if current incident (ID) not found
        var incident = incidents.find { it.id == selectedId } ?: EmptyIncident
        if (incident == EmptyIncident && incidents.isNotEmpty()) {
            incident = incidents[0]
        }
        selectedId = incident.id

        incidentSelector.incident = incident

        updateAppTitle()

        if (selectedIncidentId < 0) IncidentsData.Empty
        else IncidentsData.Incidents(incidents)
    }.stateIn(
        scope = viewModelScope,
        initialValue = IncidentsData.Loading,
        started = SharingStarted.WhileSubscribed(),
    )

    var casesSearchQuery = mutableStateOf("")
        private set

    // TODO Use constant for debounce
    private val filteredResults = flowOf(casesSearchQuery)
        .debounce(200)
        .map { it.value.trim() }
        .distinctUntilChanged()
        .map {
            // TODO Filter if (search is is open and) query is not defined
        }
        .launchIn(viewModelScope)

    fun updateCasesSearchQuery(q: String) {
        casesSearchQuery.value = q
    }

    fun selectIncident(incident: Incident) {
        // TODO Atomic set
        if (incidentsData.value is IncidentsData.Incidents) {
            val incidents = (incidentsData.value as IncidentsData.Incidents).incidents
            val verifiedIncident = incidents.find { it.id == incident.id }
            if (verifiedIncident != null) {
                incidentSelector.incident = incident
                updateAppTitle()
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