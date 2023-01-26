package com.crisiscleanup.feature.cases

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderBar
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
    private val incidentsRepository: IncidentsRepository,
    val appHeaderBar: AppHeaderBar,
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

    var selectedIncident: MutableState<Incident> = mutableStateOf(EmptyIncident)
        private set

    // TODO Save and load from prefs later.
    var incidentId = mutableStateOf(EmptyIncident.id)
        private set

    val incidentsData = incidentsRepository.incidents.map { incidents ->
        var selectedId = incidentId.value

        // Update incident data or select first if current incident (ID) not found
        var incident = incidents.find { it.id == selectedId } ?: EmptyIncident
        if (incident == EmptyIncident && incidents.isNotEmpty()) {
            incident = incidents[0]
        }
        selectedId = incident.id

        // TODO Assign atomically (or use a single value)
        incidentId.value = selectedId
        selectedIncident.value = incident

        appHeaderBar.setTitle(selectedIncident.value.name)

        if (incidentId.value < 0) IncidentsData.Empty
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
}

sealed interface IncidentsData {
    object Loading : IncidentsData

    data class Incidents(
        val incidents: List<Incident>,
    )

    object Empty : IncidentsData
}