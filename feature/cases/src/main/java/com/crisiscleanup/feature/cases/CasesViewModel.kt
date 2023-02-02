package com.crisiscleanup.feature.cases

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.feature.cases.model.MapViewCameraBounds
import com.crisiscleanup.feature.cases.model.MapViewCameraBoundsDefault
import com.crisiscleanup.feature.cases.model.asLatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    incidentSelector: IncidentSelector,
    private val appHeaderUiState: AppHeaderUiState,
    appPreferencesRepository: LocalAppPreferencesRepository,
) : ViewModel() {
    val incidentsData = IncidentsDataLoader(
        viewModelScope,
        incidentsRepository,
        incidentSelector,
        appPreferencesRepository,
    ).data

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

    private val isUpdatingCameraBounds = MutableStateFlow(false)

    val isLoading = combine(
        incidentsRepository.isLoading,
        worksitesRepository.isLoading,
        isUpdatingCameraBounds
    ) { incidentsLoading,
        worksitesLoading,
        isUpdatingCameraBounds ->
        incidentsLoading ||
                worksitesLoading ||
                isUpdatingCameraBounds
    }

    val worksitesMapMarkers = incidentSelector.incidentId.flatMapLatest {
        if (it == EmptyIncident.id) {
            flowOf(emptyList())
        } else {
            // TODO Combine with search query and filters into single state. Load from network as available and necessary.
            worksitesRepository.getWorksitesMapVisual(it)
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed()
    )

    // TODO Reset map tiler

    // TODO Create provider to
    //      Restore last map location if cached
    //      Move to device's physical location if first load?
    //      Use incident's location if...
    val mapCameraBounds = worksitesMapMarkers.flatMapLatest {
        if (it.isEmpty()) {
            flowOf(MapViewCameraBoundsDefault)
        } else {
            isUpdatingCameraBounds.value = true

            // TODO Use incident's location repository instead
            val builder = it.fold(
                LatLngBounds.builder(),
            ) { acc, curr -> acc.include(curr.asLatLng()) }
            val bounds = MapViewCameraBounds(builder.build())

            isUpdatingCameraBounds.value = false

            flowOf(bounds)
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = MapViewCameraBoundsDefault,
        started = SharingStarted.WhileSubscribed()
    )

    init {
        incidentSelector.incident.onEach { it -> appHeaderUiState.setTitle(it.name) }
            .launchIn(viewModelScope)
    }

    var casesSearchQuery = mutableStateOf("")
        private set

    fun updateCasesSearchQuery(q: String) {
        casesSearchQuery.value = q
    }
}
