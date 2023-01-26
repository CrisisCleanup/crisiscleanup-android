package com.crisiscleanup.feature.cases

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderBar
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.model.data.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    val appHeaderBar: AppHeaderBar,
) : ViewModel() {
    val isLoadingIncidents = incidentsRepository.isLoading

    // TODO Save and load from prefs later.
    var incidentId = mutableStateOf(-1L)
        private set

    val incidentsData = incidentsRepository.incidents.map {
        val ids = it.map(Incident::id)
        if (!ids.contains(incidentId.value)) {
            incidentId.value = -1L
        }

        if (incidentId.value < 0) {
            if (it.isNotEmpty()) {
                incidentId.value = it[0].id
            }
        }

        if (incidentId.value < 0) IncidentsData.Empty
        else IncidentsData.Incidents(it)
    }.stateIn(
        scope = viewModelScope,
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