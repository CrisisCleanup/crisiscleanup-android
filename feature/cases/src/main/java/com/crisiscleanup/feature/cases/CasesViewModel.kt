package com.crisiscleanup.feature.cases

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.domain.LoadIncidentDataUseCase
import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentLocation
import com.crisiscleanup.feature.cases.model.CoordinateBounds
import com.crisiscleanup.feature.cases.model.MapViewCameraBounds
import com.crisiscleanup.feature.cases.model.MapViewCameraBoundsDefault
import com.crisiscleanup.feature.cases.model.asWorksiteGoogleMapMark
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    locationsRepository: LocationsRepository,
    worksitesRepository: WorksitesRepository,
    incidentSelector: IncidentSelector,
    appHeaderUiState: AppHeaderUiState,
    loadIncidentDataUseCase: LoadIncidentDataUseCase,
    mapCaseDotProvider: MapCaseDotProvider,
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

    val incidentLocationBounds = incidentSelector.incident.map { incident ->
        var bounds: LatLngBounds = MapViewCameraBoundsDefault.bounds

        isUpdatingCameraBounds.value = true
        try {
            val locations =
                locationsRepository.getLocations(incident.locations.map(IncidentLocation::location))
            if (locations.isNotEmpty()) {
                // Assumes coordinates and multiCoordinates are lng-lat ordered pairs
                val coordinates = locations.mapNotNull {
                    it.multiCoordinates?.flatten() ?: it.coordinates
                }.flatten()
                val latLngs = mutableListOf<LatLng>()
                for (i in 1 until coordinates.size step 2) {
                    latLngs.add(LatLng(coordinates[i], coordinates[i - 1]))
                }
                val locationBounds =
                    latLngs.fold(LatLngBounds.builder()) { acc, latLng -> acc.include(latLng) }
                bounds = locationBounds.build()
            }

            if (bounds == MapViewCameraBoundsDefault.bounds) {
                // TODO Report/log location bounds are missing
                val worksites = worksitesRepository.getWorksites(incident.id, 100, 0).first()
                val locationBounds =
                    worksites.map { LatLng(it.latitude, it.longitude) }
                        .fold(LatLngBounds.builder()) { acc, latLng -> acc.include(latLng) }
                bounds = locationBounds.build()
            }

            if (bounds.southwest.latitude == bounds.northeast.latitude ||
                bounds.southwest.longitude == bounds.northeast.longitude
            ) {
                // TODO Add padding to bounds
            }

        } finally {
            isUpdatingCameraBounds.value = false
        }

        MapViewCameraBounds(bounds)
    }.stateIn(
        scope = viewModelScope,
        initialValue = MapViewCameraBoundsDefault,
        started = SharingStarted.WhileSubscribed()
    )

    val worksitesMapMarkers = qsm.worksiteQueryState
        // TODO Make debounce a parameter
        .debounce(250)
        .flatMapLatest {
            skipMarksAutoBounding = false

            Log.w("query", "Query state change $it")
            val id = it.incidentId
            if (isTableView.value || id == EmptyIncident.id) {
                flowOf(emptyList())
            } else {
                // TODO Defer to tiler when zoom is far out
                val visuals = worksitesRepository.getWorksitesMapVisual(
                    id,
                    it.coordinateBounds.southWest.latitude,
                    it.coordinateBounds.northEast.latitude,
                    it.coordinateBounds.southWest.longitude,
                    it.coordinateBounds.northEast.longitude,
                    100,
                    0,
                ).map { marks ->
                    marks.map { mark -> mark.asWorksiteGoogleMapMark(mapCaseDotProvider) }
                }
                Log.w("Query", "Result ${visuals.first().size}")
                visuals
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed()
        )

    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?, isActiveChange: Boolean
    ) {
        qsm.mapZoom.value = cameraPosition.zoom

        projection?.let {
            val visibleBounds = it.visibleRegion.latLngBounds
            qsm.mapBounds.value = CoordinateBounds(
                visibleBounds.southwest,
                visibleBounds.northeast,
            )
        }

        if (isActiveChange) {
            skipMarksAutoBounding = true
        }
    }
}
