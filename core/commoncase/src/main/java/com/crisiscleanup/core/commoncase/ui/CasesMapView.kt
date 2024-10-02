package com.crisiscleanup.core.commoncase.ui

import android.util.Log
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.commoncase.model.WorksiteGoogleMapMark
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBounds
import com.crisiscleanup.core.mapmarker.model.MapViewCameraBoundsDefault
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoom
import com.crisiscleanup.core.mapmarker.model.MapViewCameraZoomDefault
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.TileOverlayState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberTileOverlayState
import com.crisiscleanup.core.mapmarker.R as mapMarkerR

@Composable
fun BoxScope.CasesMapView(
    modifier: Modifier = Modifier,
    mapCameraBounds: MapViewCameraBounds = MapViewCameraBoundsDefault,
    mapCameraZoom: MapViewCameraZoom = MapViewCameraZoomDefault,
    isMapBusy: Boolean = false,
    worksitesOnMap: List<WorksiteGoogleMapMark> = emptyList(),
    tileChangeValue: Long = -1,
    clearTileLayer: () -> Boolean = { false },
    tileOverlayState: TileOverlayState = rememberTileOverlayState(),
    casesDotTileProvider: () -> TileProvider? = { null },
    onMapLoadStart: () -> Unit = {},
    onMapLoaded: () -> Unit = {},
    onMapCameraChange: (CameraPosition, Projection?, Boolean) -> Unit = { _, _, _ -> },
    onMarkerSelect: (WorksiteMapMark) -> Boolean = { false },
    editedWorksiteLocation: LatLng? = null,
    isMyLocationEnabled: Boolean = false,
    onEditLocationZoom: Float = 12f,
) {
    // TODO Profile and optimize recompositions when map is changed (by user) if possible.

    LaunchedEffect(Unit) {
        onMapLoadStart()
    }

    val uiSettings by rememberMapUiSettings()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            mapCameraBounds.bounds.center,
            mapCameraZoom.zoom,
        )
    }

    val mapProperties by rememberMapProperties(
        mapMarkerR.raw.map_style,
        isMyLocation = isMyLocationEnabled,
    )
    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .testTag("casesMapViewContainer"),
        uiSettings = uiSettings,
        properties = mapProperties,
        onMapLoaded = onMapLoaded,
        cameraPositionState = cameraPositionState,
    ) {
        // TODO Is it possible to cache? If so test recomposition. If not document why not.
        worksitesOnMap.forEach { mapMark ->
            Marker(
                mapMark.markerState,
                icon = mapMark.mapIcon,
                anchor = mapMark.mapIconOffset,
                onClick = { onMarkerSelect(mapMark.source) },
            )
        }

        if (tileChangeValue >= 0) {
            casesDotTileProvider()?.let {
                if (clearTileLayer()) {
                    // TODO When inspection of state and overlay is possible remove the try/catch
                    //      This is fine for now as the exception is just an escape from the method.
                    try {
                        // TODO This is not clearing as expected on API 29 at times. Unrelated to try/catch.
                        tileOverlayState.clearTileCache()
                    } catch (e: IllegalStateException) {
                        if (e.message?.contains("is not used") == true) {
                            Log.d("tile-overlay", "Ignoring unattached tile overlay state")
                        } else {
                            throw e
                        }
                    }
                }
                TileOverlay(it, tileOverlayState)
            }
        }
    }

    BusyIndicatorFloatingTopCenter(isMapBusy)

    val currentLocalDensity = LocalDensity.current
    LaunchedEffect(mapCameraBounds) {
        if (mapCameraBounds.takeApply()) {
            // TODO Make padding dp a parameter
            val padding = with(currentLocalDensity) { 8.dp.toPx() }
            val update = CameraUpdateFactory.newLatLngBounds(
                mapCameraBounds.bounds,
                padding.toInt(),
            )
            if (mapCameraBounds.durationMs > 0) {
                cameraPositionState.animate(update, mapCameraBounds.durationMs)
            } else {
                cameraPositionState.move(update)
            }
        }
    }

    LaunchedEffect(mapCameraZoom) {
        if (mapCameraZoom.takeApply()) {
            val update = CameraUpdateFactory.newLatLngZoom(
                mapCameraZoom.center,
                mapCameraZoom.zoom,
            )
            if (mapCameraZoom.durationMs > 0) {
                cameraPositionState.animate(update, mapCameraZoom.durationMs)
            } else {
                cameraPositionState.move(update)
            }
        }
    }

    editedWorksiteLocation?.let {
        LaunchedEffect(Unit) {
            val update = CameraUpdateFactory.newLatLngZoom(it, onEditLocationZoom)
            cameraPositionState.move(update)
        }
    }

    val isMovingByGesture by remember {
        derivedStateOf {
            cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        }
    }
    onMapCameraChange(
        cameraPositionState.position,
        cameraPositionState.projection,
        isMovingByGesture,
    )
}
