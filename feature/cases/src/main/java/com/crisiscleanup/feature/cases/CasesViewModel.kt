package com.crisiscleanup.feature.cases

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
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
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    private val locationsRepository: LocationsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val incidentSelector: IncidentSelector,
    appHeaderUiState: AppHeaderUiState,
    loadIncidentDataUseCase: LoadIncidentDataUseCase,
    mapCaseDotProvider: MapCaseDotProvider,
    private val mapTileRenderer: CaseDotsMapTileRenderer,
    private val tileProvider: TileProvider,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val incidentsData = loadIncidentDataUseCase()

    private val qsm = CasesQueryStateManager(
        incidentSelector,
        appHeaderUiState,
        viewModelScope,
    )

    val isTableView = qsm.isTableView

    val overviewTileDataSize = mapTileRenderer.tileDataSize

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

    init {
        mapTileRenderer.setScope(viewModelScope)

        incidentSelector.incidentId.onEach {
            mapTileRenderer.setIncident(it)
        }.launchIn(viewModelScope)
    }

    val isLoading = combine(
        incidentsRepository.isLoading,
        worksitesRepository.isLoading,
        isUpdatingCameraBounds,
        mapTileRenderer.isBusy,
    ) {
            incidentsLoading,
            worksitesLoading,
            isUpdatingCameraBounds,
            rendererBusy,
        ->
        incidentsLoading ||
                worksitesLoading ||
                isUpdatingCameraBounds ||
                rendererBusy
    }

    val incidentLocationBounds = incidentSelector.incident.map { incident ->
        withContext(ioDispatcher) {
            var bounds = MapViewCameraBoundsDefault.bounds

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
                    val worksites = worksitesRepository.getWorksitesMapVisual(incident.id, 100, 0)
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
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = MapViewCameraBoundsDefault,
        started = SharingStarted.WhileSubscribed()
    )

    val worksitesMapMarkers = qsm.worksiteQueryState
        // TODO Make debounce a parameter
        .debounce(250)
        .flatMapLatest {
            val id = it.incidentId
            val skipMarkers = isTableView.value ||
                    id == EmptyIncident.id ||
                    mapTileRenderer.rendersAt(it.zoom)
            Log.w("query", "Query state change ${it.zoom} $it")
            if (skipMarkers) {
                flowOf(emptyList())
            } else {
                val visuals = worksitesRepository.streamWorksitesMapVisual(
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
            started = SharingStarted.WhileSubscribed(),
        )

    fun overviewMapTileProvider(): TileProvider? {
        val ignoreTiling = isTableView.value ||
                incidentSelector.incidentId.value == EmptyIncident.id ||
                !mapTileRenderer.rendersAt(qsm.mapZoom.value)
        return if (ignoreTiling) null else tileProvider
    }

    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean
    ) {
        qsm.mapZoom.value = cameraPosition.zoom

        projection?.let {
            val visibleBounds = it.visibleRegion.latLngBounds
            qsm.mapBounds.value = CoordinateBounds(
                visibleBounds.southwest,
                visibleBounds.northeast,
            )
        }
    }
}
