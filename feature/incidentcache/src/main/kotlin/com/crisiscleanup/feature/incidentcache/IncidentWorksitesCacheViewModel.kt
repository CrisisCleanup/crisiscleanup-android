package com.crisiscleanup.feature.incidentcache

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.common.subscribedReplay
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.smallOffset
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.InitialIncidentWorksitesCachePreferences
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import com.crisiscleanup.core.common.R as commonR

@HiltViewModel
class IncidentWorksitesCacheViewModel @Inject constructor(
    incidentSelector: IncidentSelector,
    private val incidentCacheRepository: IncidentCacheRepository,
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    appEnv: AppEnv,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(), BoundedRegionDataEditor {
    val isNotProduction = appEnv.isNotProduction

    val incident = incidentSelector.incident

    val isSyncing = incidentCacheRepository.isSyncingActiveIncident
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = subscribedReplay(),
        )

    val lastSynced = incident
        .flatMapLatest { incident ->
            incidentCacheRepository.streamSyncStats(incident.id)
                .mapNotNull {
                    it?.lastUpdated?.relativeTime
                }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = subscribedReplay(),
        )

    val isUpdatingSyncParameters = MutableStateFlow(false)
    val syncingParameters = incidentCacheRepository.cachePreferences
        .stateIn(
            scope = viewModelScope,
            initialValue = InitialIncidentWorksitesCachePreferences,
            started = subscribedReplay(),
        )

    override val centerCoordinates = MutableStateFlow(LatLng(0.0, 0.0))

    private var zoomCache = defaultMapZoom
    override val mapMarkerIcon: BitmapDescriptor?

    private val defaultMapZoom: Float
        get() {
            val zoom = 19
            return zoom + (Math.random() * 1e-3).toFloat()
        }

    private var isMapLoaded = false

    override val mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)

    private var hasReceivedMapChange = AtomicBoolean(false)

    /**
     * Indicates the map was manually moved
     */
    private val isMapMoved = AtomicBoolean(false)

    override val showExplainPermissionLocation = mutableStateOf(false)

    init {
        // TODO Common dimensions
        val pinMarkerSize = Pair(32f, 48f)
        mapMarkerIcon = drawableResourceBitmapProvider.getIcon(
            commonR.drawable.cc_foreground_pin,
            pinMarkerSize,
        )

        permissionManager.permissionChanges.map {
            if (it == locationPermissionGranted && !isMapMoved.getAndSet(false)) {
                setMyLocationCoordinates()
            }
        }.launchIn(viewModelScope)
    }

    private fun updatePreferences(
        isPaused: Boolean,
        isRegionBounded: Boolean,
    ) {
        if (!isUpdatingSyncParameters.compareAndSet(expect = false, update = true)) {
            return
        }

        val parameters = syncingParameters.value
        val radius = if (isRegionBounded && parameters.regionRadiusMiles <= 0) {
            // TODO Use constant
            10f
        } else {
            parameters.regionRadiusMiles
        }
        val updatedParameters = parameters.copy(
            isPaused = isPaused,
            isRegionBounded = isRegionBounded,
            regionRadiusMiles = radius,
        )

        viewModelScope.launch(ioDispatcher) {
            try {
                incidentCacheRepository.updateCachePreferences(updatedParameters)
            } finally {
                isUpdatingSyncParameters.value = false
            }
        }
    }

    fun resumeCachingCases() {
        updatePreferences(isPaused = false, isRegionBounded = false)

        // TODO Restart caching
    }

    fun pauseCachingCases() {
        updatePreferences(isPaused = true, isRegionBounded = false)

        // TODO Cancel ongoing
    }

    fun boundCachingCases() {
        // TODO Region parameters
        updatePreferences(isPaused = false, isRegionBounded = true)

        // TODO Restart caching if region parameters are defined
    }

    fun resetCaching() {
        viewModelScope.launch(ioDispatcher) {
            incidentCacheRepository.resetIncidentSyncStats(incident.value.id)
        }
    }

    private fun centerCoordinatesZoom(durationMs: Int = 0) = MapViewCameraZoom(
        centerCoordinates.value.smallOffset(),
        defaultMapZoom,
        durationMs,
    )

    // BoundedRegionDataEditor

    override fun onMapLoaded() {
        mapCameraZoom.value = centerCoordinatesZoom()
    }

    override fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean,
    ) {
        zoomCache = cameraPosition.zoom

        projection?.let {
            if (hasReceivedMapChange.compareAndSet(false, true)) {
                mapCameraZoom.value = centerCoordinatesZoom()
            } else {
                if (isMapLoaded) {
                    val center = it.visibleRegion.latLngBounds.center
                    centerCoordinates.value = center
                }
            }
        }

        if (isActiveChange) {
            isMapMoved.set(true)
        }
    }

    override fun centerOnLocation() {
        val coordinates = centerCoordinates.value.smallOffset()
        mapCameraZoom.value = MapViewCameraZoom(coordinates, zoomCache)
    }

    private fun setMyLocationCoordinates() {
        viewModelScope.launch(ioDispatcher) {
            locationProvider.getLocation()?.let {
                val coordinates = it.toLatLng().smallOffset()
                centerCoordinates.value = coordinates
                mapCameraZoom.value = MapViewCameraZoom(coordinates, defaultMapZoom)
                // TODO Is isUserAction correct when permission must be granted?
            }
        }
    }

    override fun useMyLocation() {
        when (permissionManager.requestLocationPermission()) {
            PermissionStatus.Granted -> {
                setMyLocationCoordinates()
            }

            PermissionStatus.ShowRationale -> {
                showExplainPermissionLocation.value = true
            }

            PermissionStatus.Requesting -> {
                isMapMoved.set(false)
            }

            PermissionStatus.Denied,
            PermissionStatus.Undefined,
            -> {
                // Ignore these statuses as they're not important
            }
        }
    }
}
