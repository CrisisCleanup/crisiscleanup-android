package com.crisiscleanup.feature.team

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.commoncase.CasesConstant.MAP_MARKERS_ZOOM_LEVEL
import com.crisiscleanup.core.commoncase.CasesCounter
import com.crisiscleanup.core.commoncase.map.CasesMapBoundsManager
import com.crisiscleanup.core.commoncase.map.CasesMapTileLayerManager
import com.crisiscleanup.core.commoncase.map.CasesOverviewMapTileRenderer
import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.commoncase.model.WorksiteGoogleMapMark
import com.crisiscleanup.core.commoncase.ui.CasesAction
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.DataProgressMetrics
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.TileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

interface TeamCaseMapManager {
    val isMyLocationEnabled: StateFlow<Boolean>

    val clearTileLayer: Boolean

    val filtersCount: Flow<Int>
    val isMapBusy: Flow<Boolean>
    val casesCountMapText: StateFlow<String>
    val worksitesMapMarkers: StateFlow<List<WorksiteGoogleMapMark>>
    val incidentLocationBounds: StateFlow<MapViewCameraBounds>
    val mapCameraZoom: StateFlow<MapViewCameraZoom>
    val overviewTileDataChange: State<Long>

    val incidentId: Long
    val dataProgress: StateFlow<DataProgressMetrics>

    val isLoadingData: Flow<Boolean>

    fun toggleLayersView()
    fun zoomToInteractive()
    fun zoomToIncidentBounds()
    fun zoomIn()
    fun zoomOut()
    fun onCasesAction(action: CasesAction)

    fun overviewMapTileProvider(): TileProvider
    fun onMapLoadStart()
    fun onMapLoaded()
    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean,
    )

    fun setMapToMyCoordinates()
    suspend fun setTileRendererLocation()

    fun grantAccessToDeviceLocation()

    fun syncWorksitesDelta(forceRefreshAll: Boolean)
}

internal class CreateEditTeamCaseMapManager(
    private val qsm: TeamCasesQueryStateManager,
    override val dataProgress: StateFlow<DataProgressMetrics>,
    override val isLoadingData: Flow<Boolean>,
    isGeneratingWorksiteMarkers: StateFlow<Boolean>,
    override val worksitesMapMarkers: StateFlow<List<WorksiteGoogleMapMark>>,
    override val isMyLocationEnabled: StateFlow<Boolean>,
    private val incidentSelector: IncidentSelector,
    private val tileProvider: TileProvider,
    private val mapTileRenderer: CasesOverviewMapTileRenderer,
    private val mapBoundsManager: CasesMapBoundsManager,
    filterRepository: CasesFilterRepository,
    casesCounter: CasesCounter,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    private val syncPuller: SyncPuller,
    private val coroutineScope: CoroutineScope,
    private val logger: AppLogger,
) : TeamCaseMapManager {

    var showExplainPermissionLocation by mutableStateOf(false)

    private val casesMapTileManager = CasesMapTileLayerManager(
        coroutineScope,
        incidentSelector,
        mapBoundsManager,
        logger,
    )
    override val clearTileLayer: Boolean
        get() = casesMapTileManager.clearTileLayer

    override val filtersCount = filterRepository.filtersCount

    override val isMapBusy = combine(
        mapBoundsManager.isDeterminingBounds,
        mapTileRenderer.isBusy,
        isGeneratingWorksiteMarkers,
    ) { b0, b1, b2 -> b0 || b1 || b2 }
    override val casesCountMapText = casesCounter.casesCountMapText

    override val incidentLocationBounds = mapBoundsManager.mapCameraBounds

    private var mapCameraZoomInternal = MutableStateFlow(MapViewCameraZoomDefault)
    override val mapCameraZoom: StateFlow<MapViewCameraZoom> = mapCameraZoomInternal
    override val overviewTileDataChange = casesMapTileManager.overviewTileDataChange

    override val incidentId: Long
        get() = incidentSelector.incidentId.value

    override fun toggleLayersView() {
        logger.logDebug("toggleLayersView awaiting implementation")
    }

    private fun adjustMapZoom(zoomLevel: Float) {
        if (zoomLevel < 3 || zoomLevel > 17) {
            return
        }

        mapCameraZoomInternal.value = MapViewCameraZoom(
            mapBoundsManager.centerCache,
            (zoomLevel + Math.random() * 1e-3).toFloat(),
        )
    }

    override fun zoomToInteractive() = adjustMapZoom(MAP_MARKERS_ZOOM_LEVEL + 0.5f)

    override fun zoomToIncidentBounds() = mapBoundsManager.restoreIncidentBounds()

    override fun zoomIn() = adjustMapZoom(qsm.mapZoom.value + 1)

    override fun zoomOut() = adjustMapZoom(qsm.mapZoom.value - 1)

    override fun onCasesAction(action: CasesAction) {
        logger.logDebug("Unexpected team Case map action $action")
    }

    override fun overviewMapTileProvider(): TileProvider {
        // Do not try and be efficient here by returning null when tiling is not necessary (when used in compose).
        // Doing so will cause errors with TileOverlay and TileOverlayState#clearTileCache.
        return tileProvider
    }

    override suspend fun setTileRendererLocation() {
        mapTileRenderer.setLocation(locationProvider.getLocation())
    }

    override fun onMapLoadStart() {
        // Do not try to optimize delaying marker loading until the map is loaded.
        // A lot of state is changing. Refactor or keep simple.
    }

    override fun onMapLoaded() {
        if (mapBoundsManager.onMapLoaded()) {
            mapBoundsManager.restoreBounds()
        }
    }

    override fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean,
    ) {
        qsm.mapZoom.value = cameraPosition.zoom

        if (mapBoundsManager.isMapLoaded.value) {
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

    override fun setMapToMyCoordinates() {
        coroutineScope.launch {
            locationProvider.getLocation()?.let { myLocation ->
                mapCameraZoomInternal.value = MapViewCameraZoom(
                    myLocation.toLatLng(),
                    (11f + Math.random() * 1e-3).toFloat(),
                )
            }
        }
    }

    override fun grantAccessToDeviceLocation() {
        when (permissionManager.requestLocationPermission()) {
            PermissionStatus.Granted -> {
                setMapToMyCoordinates()
            }

            PermissionStatus.ShowRationale -> {
                showExplainPermissionLocation = true
            }

            PermissionStatus.Requesting,
            PermissionStatus.Denied,
            PermissionStatus.Undefined,
            -> {
                // Ignore these statuses as they're not important
            }
        }
    }

    override fun syncWorksitesDelta(forceRefreshAll: Boolean) {
        syncPuller.appPullIncidentWorksitesDelta(forceRefreshAll)
    }
}
