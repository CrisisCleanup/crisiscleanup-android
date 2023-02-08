package com.crisiscleanup.feature.cases

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.domain.LoadIncidentDataUseCase
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.feature.cases.model.CoordinateBounds
import com.crisiscleanup.feature.cases.model.MapViewCameraBounds
import com.crisiscleanup.feature.cases.model.MapViewCameraBoundsDefault
import com.crisiscleanup.feature.cases.model.asWorksiteGoogleMapMark
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    incidentSelector: IncidentSelector,
    appHeaderUiState: AppHeaderUiState,
    loadIncidentDataUseCase: LoadIncidentDataUseCase,
) : ViewModel() {
    val incidentsData = loadIncidentDataUseCase()

    private val qsm = CasesQueryStateManager(
        incidentSelector,
        appHeaderUiState,
        viewModelScope,
    )

    val isTableView = qsm.isTableView

    // TODO Is it possible use stateFlow in Compose deferring evaluation until needed in the hierarchy like with a remembered lambda? Maybe not so research and test with the layout inspector.
    var casesSearchQuery = mutableStateOf("")
        private set

    fun updateCasesSearchQuery(q: String) {
        casesSearchQuery.value = q
        qsm.casesSearchQueryFlow.value = q
    }

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

    private var skipMarksAutoBounding = false

    val worksitesMapMarkers = qsm.worksiteQueryState.flatMapLatest {
        skipMarksAutoBounding = false

        Log.w("query", "Query state change $it")
        val id = it.incidentId
        if (isTableView.value || id == EmptyIncident.id) {
            flowOf(emptyList())
        } else {
            worksitesRepository.getWorksitesMapVisual(
                id,
                it.coordinateBounds.southWest.latitude,
                it.coordinateBounds.northEast.latitude,
                it.coordinateBounds.southWest.longitude,
                it.coordinateBounds.northEast.longitude,
                100,
                0,
            ).map { marks ->
                marks.map(WorksiteMapMark::asWorksiteGoogleMapMark)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed()
    )

    // TODO Create provider (if not deleted later) to
    //      Restore last map location if cached
    //      Move to device's physical location if first load?
    //      Use incident's location if...
    val mapCameraBounds = worksitesMapMarkers
        // TODO Make configurable.
        .debounce(150)
        .flatMapLatest {
            if (isTableView.value || it.isEmpty() || skipMarksAutoBounding) {
                flowOf(MapViewCameraBoundsDefault)
            } else {
                isUpdatingCameraBounds.value = true

                // TODO Use incident's location repository instead
                val builder = it.fold(
                    LatLngBounds.builder(),
                ) { acc, curr -> acc.include(curr.latLng) }
                val bounds = MapViewCameraBounds(builder.build())

                isUpdatingCameraBounds.value = false

                flowOf(bounds)
            }
        }.stateIn(
            scope = viewModelScope,
            initialValue = MapViewCameraBoundsDefault,
            started = SharingStarted.WhileSubscribed()
        )

    fun onMapCameraChange(projection: Projection?, isActiveChange: Boolean) {
        projection?.let {
            val visibleBounds = it.visibleRegion.latLngBounds
            qsm.mapBounds.value = CoordinateBounds(
                visibleBounds.southwest,
                visibleBounds.northeast
            )
        }

        if (isActiveChange) {
            skipMarksAutoBounding = true
        }
    }
}
