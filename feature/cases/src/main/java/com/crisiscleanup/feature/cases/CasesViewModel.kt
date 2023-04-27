package com.crisiscleanup.feature.cases

import android.content.ComponentCallbacks2
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.*
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import com.crisiscleanup.core.common.event.TrimMemoryListener
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.data.util.IncidentDataPullReporter
import com.crisiscleanup.core.data.util.IncidentDataPullStats
import com.crisiscleanup.core.domain.LoadIncidentDataUseCase
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.feature.cases.map.*
import com.crisiscleanup.feature.cases.model.*
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import com.crisiscleanup.core.commonassets.R as commonAssetsR

@HiltViewModel
class CasesViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    locationsRepository: LocationsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val incidentSelector: IncidentSelector,
    loadIncidentDataUseCase: LoadIncidentDataUseCase,
    dataPullReporter: IncidentDataPullReporter,
    private val mapCaseIconProvider: MapCaseIconProvider,
    private val mapTileRenderer: CasesOverviewMapTileRenderer,
    private val tileProvider: TileProvider,
    private val worksiteLocationEditor: WorksiteLocationEditor,
    private val syncPuller: SyncPuller,
    private val resourceProvider: AndroidResourceProvider,
    appMemoryStats: AppMemoryStats,
    trimMemoryEventManager: TrimMemoryEventManager,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
) : ViewModel(), TrimMemoryListener {
    val incidentsData = loadIncidentDataUseCase()

    val incidentId: Long
        get() = incidentSelector.incidentId.value

    val disasterIconResId = incidentSelector.incident.map { getDisasterIcon(it.disaster) }
        .stateIn(
            scope = viewModelScope,
            initialValue = commonAssetsR.drawable.ic_disaster_other,
            started = SharingStarted.WhileSubscribed(),
        )

    private val qsm = CasesQueryStateManager(
        incidentSelector,
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

    val editedWorksiteLocation: LatLng?
        get() = worksiteLocationEditor.takeEditedLocation()?.let { LatLng(it.first, it.second) }

    val showDataProgress = dataPullReporter.incidentDataPullStats.map { it.isOngoing }
    val dataProgress = dataPullReporter.incidentDataPullStats.map { it.progress }

    private var _mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)
    val mapCameraZoom = _mapCameraZoom.asStateFlow()

    private val mapBoundsManager = CasesMapBoundsManager(
        viewModelScope,
        incidentSelector,
        locationsRepository,
        ioDispatcher,
        logger,
    )

    var incidentLocationBounds = mapBoundsManager.mapCameraBounds

    val isSyncingIncidents = incidentsRepository.isLoading
    private val isGeneratingWorksiteMarkers = MutableStateFlow(false)
    val isMapBusy = combine(
        mapBoundsManager.isDeterminingBounds,
        mapTileRenderer.isBusy,
        isGeneratingWorksiteMarkers,
    ) {
            isMapBounding,
            rendererBusy,
            isGeneratingMarkers,
        ->
        isMapBounding ||
                rendererBusy ||
                isGeneratingMarkers
    }

    private val mapMarkerManager = CasesMapMarkerManager(
        worksitesRepository,
        appMemoryStats,
        logger,
    )

    private var _hiddenMarkersMessage = ""
    val hiddenMarkersMessage: String
        get() = if (mapTileRenderer.rendersAt(qsm.mapZoom.value)) "" else _hiddenMarkersMessage

    val worksitesMapMarkers = qsm.worksiteQueryState
        // TODO Make debounce a parameter
        .debounce(250)
        .flatMapLatest { wqs ->
            val id = wqs.incidentId
            val skipMarkers = isTableView.value ||
                    id == EmptyIncident.id ||
                    mapTileRenderer.rendersAt(wqs.zoom)

            logger.logDebug("Query state change", wqs)

            if (skipMarkers) {
                flowOf(emptyList())
            } else {
                isGeneratingWorksiteMarkers.value = true
                withContext(ioDispatcher) {
                    try {
                        generateWorksiteMarkers(wqs)
                    } finally {
                        isGeneratingWorksiteMarkers.value = false
                    }
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
        mapBoundsManager,
        logger,
    )
    val overviewTileDataChange = casesMapTileManager.overviewTileDataChange
    val clearTileLayer: Boolean
        get() = casesMapTileManager.clearTileLayer

    private val tileClearRefreshInterval = 5.seconds
    private var tileRefreshedInstant: Instant = Instant.fromEpochSeconds(0)
    private var tileClearWorksitesCount = 0

    private val incidentWorksitesCount = incidentSelector.incidentId.flatMapLatest { id ->
        worksitesRepository.streamIncidentWorksitesCount(id)
            .map { count -> IncidentIdWorksiteCount(id, count) }
    }.shareIn(
        scope = viewModelScope,
        replay = 1,
        started = SharingStarted.WhileSubscribed(1_000)
    )

    init {
        trimMemoryEventManager.addListener(this)

        mapTileRenderer.enableTileBoundaries()

        combine(
            incidentWorksitesCount,
            dataPullReporter.incidentDataPullStats,
            ::Pair,
        )
            .debounce(16)
            .throttleLatest(1_000)
            .onEach { refreshTiles(it.first, it.second) }
            .launchIn(viewModelScope)
    }

    fun refreshIncidentsData() {
        syncPuller.appPull(true, cancelOngoing = true)
    }

    fun overviewMapTileProvider(): TileProvider {
        // Do not try and be efficient here by returning null when tiling is not necessary (when used in compose).
        // Doing so will cause errors with TileOverlay and TileOverlayState#clearTileCache.
        return tileProvider
    }

    private suspend fun generateWorksiteMarkers(wqs: WorksiteQueryState) = coroutineScope {
        val id = wqs.incidentId
        val sw = wqs.coordinateBounds.southWest
        val ne = wqs.coordinateBounds.northEast
        val marksQuery = mapMarkerManager.queryWorksitesInBounds(id, sw, ne)
        val visuals = marksQuery.first.map { mark ->
            mark.asWorksiteGoogleMapMark(mapCaseIconProvider)
        }

        val hiddenWorksites = marksQuery.second - visuals.size
        _hiddenMarkersMessage = if (hiddenWorksites > 0) resourceProvider.getString(
            R.string.worksite_markers_hidden,
            hiddenWorksites
        )
        else ""

        flowOf(visuals)
    }

    fun onMapLoaded() {
        if (mapBoundsManager.onMapLoaded()) {
            mapBoundsManager.restoreBounds()
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

    private fun adjustMapZoom(zoomLevel: Float) {
        if (zoomLevel < 3 || zoomLevel > 17) {
            return
        }

        _mapCameraZoom.value = MapViewCameraZoom(
            mapBoundsManager.centerCache,
            (zoomLevel + Math.random() * 1e-3).toFloat(),
        )
    }

    fun zoomIn() = adjustMapZoom(qsm.mapZoom.value + 1)

    fun zoomOut() = adjustMapZoom(qsm.mapZoom.value - 1)

    fun zoomToIncidentBounds() {
        mapBoundsManager.restoreIncidentBounds()
    }

    fun zoomToInteractive() = adjustMapZoom(mapTileRenderer.zoomThreshold + 1f)

    private suspend fun refreshTiles(
        idCount: IncidentIdWorksiteCount,
        pullStats: IncidentDataPullStats,
    ) = coroutineScope {
        var refreshTiles = true
        var clearCache = false

        pullStats.run {
            if (!isStarted || idCount.id != incidentId) {
                return@run
            }

            refreshTiles = isEnded
            clearCache = isEnded

            if (this.dataCount < 3000) {
                return@run
            }

            val now = Clock.System.now()
            if (!refreshTiles && progress > saveStartedAmount) {
                val sinceLastRefresh = now - tileRefreshedInstant
                val projectedDelta = projectedFinish - now
                refreshTiles = now - pullStart > tileClearRefreshInterval &&
                        sinceLastRefresh > tileClearRefreshInterval &&
                        projectedDelta > tileClearRefreshInterval
                if (idCount.count - tileClearWorksitesCount >= 6000 &&
                    dataCount - tileClearWorksitesCount > 3000
                ) {
                    clearCache = true
                    refreshTiles = true
                }
            }
            if (refreshTiles) {
                tileRefreshedInstant = now
            }
        }

        if (refreshTiles) {
            if (mapTileRenderer.setIncident(idCount.id, idCount.count, clearCache)) {
                clearCache = true
            }
        }

        if (clearCache) {
            tileClearWorksitesCount = idCount.count
            casesMapTileManager.clearTiles()
        } else if (refreshTiles) {
            casesMapTileManager.onTileChange()
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
