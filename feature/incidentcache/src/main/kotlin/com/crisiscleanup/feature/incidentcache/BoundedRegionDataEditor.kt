package com.crisiscleanup.feature.incidentcache

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.R
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.util.smallOffset
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

interface BoundedRegionDataEditor {
    val centerCoordinates: StateFlow<LatLng>

    val mapMarkerIcon: BitmapDescriptor?
    val mapCameraZoom: StateFlow<MapViewCameraZoom>

    val showExplainPermissionLocation: MutableState<Boolean>

    val isUserActed: Boolean

    fun onMapLoaded()

    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean,
    )

    fun centerOnLocation()
    fun setCoordinates(latitude: Double, longitude: Double)

    fun checkMyLocation(): PermissionStatus
    fun useMyLocation()
}

internal class IncidentCacheBoundedRegionDataEditor(
    private val permissionManager: PermissionManager,
    private val locationProvider: LocationProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher,
) : BoundedRegionDataEditor {
    override val centerCoordinates = MutableStateFlow(LatLng(0.0, 0.0))

    private var zoomCache = defaultMapZoom
    override val mapMarkerIcon: BitmapDescriptor?

    private var isMapLoaded = false

    override val mapCameraZoom = MutableStateFlow(MapViewCameraZoomDefault)

    private var hasReceivedMapChange = AtomicBoolean(false)

    private val isMapMoved = AtomicBoolean(false)

    override var isUserActed = false
        private set

    override val showExplainPermissionLocation = mutableStateOf(false)

    private val defaultMapZoom: Float
        get() {
            val zoom = 8
            return zoom + (Math.random() * 1e-3).toFloat()
        }

    override fun onMapLoaded() {
        mapCameraZoom.value = centerCoordinatesZoom()
        isMapLoaded = true
    }

    init {
        // TODO Common dimensions
        val pinMarkerSize = Pair(32f, 48f)
        mapMarkerIcon = drawableResourceBitmapProvider.getIcon(
            R.drawable.cc_foreground_pin,
            pinMarkerSize,
        )

        permissionManager.permissionChanges
            .onEach {
                if (it == locationPermissionGranted && !isMapMoved.getAndSet(false)) {
                    setMyLocationCoordinates()
                }
            }
            .launchIn(coroutineScope)
    }

    private fun centerCoordinatesZoom(durationMs: Int = 0) = MapViewCameraZoom(
        centerCoordinates.value.smallOffset(),
        defaultMapZoom,
        durationMs,
    )

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
            isUserActed = true
            isMapMoved.set(true)
        }
    }

    override fun centerOnLocation() {
        val coordinates = centerCoordinates.value.smallOffset()
        mapCameraZoom.value = MapViewCameraZoom(coordinates, zoomCache)
    }

    override fun setCoordinates(latitude: Double, longitude: Double) {
        centerCoordinates.value = LatLng(latitude, longitude)
    }

    private fun setMyLocationCoordinates() {
        isUserActed = true
        coroutineScope.launch(coroutineDispatcher) {
            locationProvider.getLocation()?.let {
                val coordinates = it.toLatLng().smallOffset()
                centerCoordinates.value = coordinates
                mapCameraZoom.value = MapViewCameraZoom(coordinates, defaultMapZoom)
            }
        }
    }

    private fun checkLocationPermission(setLocation: Boolean): PermissionStatus {
        if (setLocation) {
            isMapMoved.set(false)
        }
        val status = permissionManager.requestLocationPermission()
        when (status) {
            PermissionStatus.Granted -> {
                if (setLocation) {
                    setMyLocationCoordinates()
                }
            }

            PermissionStatus.ShowRationale -> {
                showExplainPermissionLocation.value = true
            }

            PermissionStatus.Requesting,
            PermissionStatus.Denied,
            PermissionStatus.Undefined,
            -> {
                // Ignore these statuses as they're not important
            }
        }

        return status
    }

    override fun checkMyLocation() = checkLocationPermission(false)

    override fun useMyLocation() {
        checkLocationPermission(true)
    }
}
