package com.crisiscleanup.core.mapmarker.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.Instant

data class MapViewCameraBounds(
    val bounds: LatLngBounds,
    val durationMs: Int = 500,
    private val timestamp: Instant = Clock.System.now(),
    private val initialApply: Boolean = true,
) {
    private val applyToMap = AtomicBoolean(initialApply)

    /**
     * @return true if bounds has yet to be taken (and applied to map) or false otherwise
     */
    fun takeApply(): Boolean = applyToMap.getAndSet(false)
}

val DefaultCoordinates = LatLng(40.272621, -96.012327)

// TODO Replace with actual location bounds from incident_id = 41762
val DefaultBounds = LatLngBounds(
    LatLng(28.598360630332458, -122.5307175747425),
    LatLng(47.27322983958189, -68.49771985492872),
)
private val swDefault = DefaultBounds.southwest
private val neDefault = DefaultBounds.northeast
val MapViewCameraBoundsDefault = MapViewCameraBounds(
    LatLngBounds(swDefault, neDefault),
    0,
    Instant.fromEpochSeconds(0),
    false,
)

data class MapViewCameraZoom(
    val center: LatLng,
    val zoom: Float = 8f,
    val durationMs: Int = 500,
    val timestamp: Instant = Clock.System.now(),
    private val initialApply: Boolean = true,
) {
    private val applyToMap = AtomicBoolean(initialApply)

    /**
     * @return true if bounds has yet to be taken (and applied to map) or false otherwise
     */
    fun takeApply(): Boolean = applyToMap.getAndSet(false)
}

val MapViewCameraZoomDefault = MapViewCameraZoom(
    swDefault,
    durationMs = 0,
    initialApply = false,
)
