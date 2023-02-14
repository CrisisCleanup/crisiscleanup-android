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
import com.crisiscleanup.feature.cases.model.CoordinateBounds
import com.crisiscleanup.feature.cases.model.asWorksiteGoogleMapMark
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    locationsRepository: LocationsRepository,
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

    /**
     * Indicates map should refresh when data size changes as tiles could have updated
     */
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

        if (!isTableView) {
            mapBoundsManager.restoreBounds()
        }
    }

    var isLayerView = mutableStateOf(false)
        private set

    fun toggleLayersView() {
        isLayerView.value = !isLayerView.value
    }

    /**
     * Guards against calling tileOverlayState.clearTileCache before TileOverlay is sufficiently loaded.
     */
    // TODO Search for updated documentation on a more elegant technique once clearTileCache has been available for some time.
    private var incidentChangeCounter = 0
    private var _clearTileLayer: Boolean = false
    var clearTileLayer: Boolean
        get() {
            if (_clearTileLayer) {
                _clearTileLayer = false
                return true
            }
            return false
        }
        private set(value) {
            _clearTileLayer = value
        }

    private val mapBoundsManager = CasesMapBoundsManager(
        viewModelScope,
        incidentSelector,
        locationsRepository,
        ioDispatcher,
    )

    var incidentLocationBounds = mapBoundsManager.mapCameraBounds

    val isLoading = combine(
        incidentsRepository.isLoading,
        worksitesRepository.isLoading,
        mapBoundsManager.isDeterminingBounds,
        mapTileRenderer.isBusy,
    ) {
            incidentsLoading,
            worksitesLoading,
            isMapBounding,
            rendererBusy,
        ->
        incidentsLoading ||
                worksitesLoading ||
                isMapBounding ||
                rendererBusy
    }

    val worksitesMapMarkers = qsm.worksiteQueryState
        // TODO Make debounce a parameter
        .debounce(250)
        .flatMapLatest { wqs ->
            val id = wqs.incidentId
            val skipMarkers = isTableView.value ||
                    id == EmptyIncident.id ||
                    mapTileRenderer.rendersAt(wqs.zoom)

            Log.w("query", "Query state change ${wqs.zoom} $wqs")

            if (skipMarkers) {
                flowOf(emptyList())
            } else {
                withContext(ioDispatcher) {
                    val sw = wqs.coordinateBounds.southWest
                    val ne = wqs.coordinateBounds.northEast
                    val visuals = worksitesRepository.getWorksitesMapVisual(
                        id,
                        sw.latitude,
                        ne.latitude,
                        sw.longitude,
                        ne.longitude,
                        // TODO Make parameter.
                        //      Decide how to prioritize when there are plenty if not already documented.
                        //      At a minimum show a visual with number of markers displayed/total.
                        1000,
                        0,
                    ).map { mark -> mark.asWorksiteGoogleMapMark(mapCaseDotProvider) }

                    Log.w("Query", "Result ${visuals.size}")

                    flowOf(visuals)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        mapTileRenderer.setScope(viewModelScope)

        qsm.worksiteQueryState
            .onEach {
                val noTiling = it.isTableView ||
                        incidentSelector.incidentId.value == EmptyIncident.id ||
                        !mapTileRenderer.rendersAt(it.zoom)
                mapTileRenderer.setRendering(!noTiling)
            }
            .launchIn(viewModelScope)

        incidentSelector.incidentId.onEach {
            mapTileRenderer.setIncident(it)

            if (it != EmptyIncident.id) {
                // Allow clearing tiles only after the incident has been changed at least once
                if (incidentChangeCounter > 0) {
                    clearTileLayer = true
                } else {
                    incidentChangeCounter++
                }
            }
        }.launchIn(viewModelScope)
    }

    fun refreshIncidentsData() {
        viewModelScope.launch {
            incidentsRepository.sync(true)
        }
    }

    fun overviewMapTileProvider(): TileProvider {
        // Do not try and be efficient here by returning null when tiling is not necessary (when used in compose).
        // Doing so will cause errors with TileOverlay and TileOverlayState#clearTileCache.
        return tileProvider
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
            mapBoundsManager.cacheBounds(visibleBounds)
        }
    }
}
