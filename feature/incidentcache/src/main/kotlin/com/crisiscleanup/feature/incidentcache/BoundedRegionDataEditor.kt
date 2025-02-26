package com.crisiscleanup.feature.incidentcache

import androidx.compose.runtime.MutableState
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.StateFlow

interface BoundedRegionDataEditor {
    val centerCoordinates: StateFlow<LatLng>

    val mapMarkerIcon: BitmapDescriptor?
    val mapCameraZoom: StateFlow<MapViewCameraZoom>

    val showExplainPermissionLocation: MutableState<Boolean>

    fun onMapLoaded()

    fun onMapCameraChange(
        cameraPosition: CameraPosition,
        projection: Projection?,
        isActiveChange: Boolean,
    )

    fun centerOnLocation()

    fun useMyLocation()
}
