package com.crisiscleanup.feature.cases

import android.content.ComponentCallbacks2
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.Syncer
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import com.crisiscleanup.core.common.event.TrimMemoryListener
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.domain.LoadIncidentDataUseCase
import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.ui.SearchManager
import com.crisiscleanup.feature.cases.map.CasesMapBoundsManager
import com.crisiscleanup.feature.cases.map.CasesMapTileLayerManager
import com.crisiscleanup.feature.cases.map.CasesOverviewMapTileRenderer
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    locationsRepository: LocationsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val incidentSelector: IncidentSelector,
    appHeaderUiState: AppHeaderUiState,
    loadIncidentDataUseCase: LoadIncidentDataUseCase,
    mapCaseDotProvider: MapCaseDotProvider,
    private val mapTileRenderer: CasesOverviewMapTileRenderer,
    private val tileProvider: TileProvider,
    searchManager: SearchManager,
    private val syncer: Syncer,
    trimMemoryEventManager: TrimMemoryEventManager,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(), TrimMemoryListener {
    val incidentsData = loadIncidentDataUseCase()

    private val qsm = CasesQueryStateManager(
        incidentSelector,
        appHeaderUiState,
        viewModelScope,
    )

    val isTableView = qsm.isTableView

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

    private val mapBoundsManager = CasesMapBoundsManager(
        viewModelScope,
        incidentSelector,
        locationsRepository,
        ioDispatcher,
    )

    var incidentLocationBounds = mapBoundsManager.mapCameraBounds

    val isSyncingIncidents = incidentsRepository.isLoading
    val isMapBusy = combine(
        mapBoundsManager.isDeterminingBounds,
        mapTileRenderer.isBusy,
    ) {
            isMapBounding,
            rendererBusy,
        ->
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
                        250,
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

    private val casesMapTileManager = CasesMapTileLayerManager(
        viewModelScope,
        incidentSelector,
        worksitesRepository,
        mapBoundsManager,
    )
    val overviewTileDataChange = casesMapTileManager.overviewTileDataChange
    val clearTileLayer: Boolean
        get() = casesMapTileManager.clearTileLayer

    init {
        trimMemoryEventManager.addListener(this)

        mapTileRenderer.enableTileBoundaries()

        searchManager.searchQueryFlow.onEach {
            qsm.casesSearchQueryFlow.value = it
        }.launchIn(viewModelScope)

        qsm.worksiteQueryState
            .onEach {
                val noTiling = it.isTableView ||
                        incidentSelector.incidentId.value == EmptyIncident.id ||
                        !mapTileRenderer.rendersAt(it.zoom) ||
                        !mapBoundsManager.isMapLoaded
                mapTileRenderer.setRenderState(noTiling, it.zoom)
                casesMapTileManager.setTilingState(noTiling, it.zoom)
            }
            .launchIn(viewModelScope)

        incidentSelector.incidentId.onEach {
            mapTileRenderer.setIncident(it)
        }.launchIn(viewModelScope)
    }

    fun refreshIncidentsData() {
        syncer.sync(true)
    }

    fun overviewMapTileProvider(): TileProvider {
        // Do not try and be efficient here by returning null when tiling is not necessary (when used in compose).
        // Doing so will cause errors with TileOverlay and TileOverlayState#clearTileCache.
        return tileProvider
    }

    fun onMapLoaded() {
        if (mapBoundsManager.onMapLoaded()) {
            casesMapTileManager.clearTiles()
        }
    }

    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean
    ) {
        qsm.mapZoom.value = cameraPosition.zoom

        if (mapBoundsManager.isMapLoaded) {
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

    // TrimMemoryListener

    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                casesMapTileManager.clearTiles()
            }
        }
    }
}
