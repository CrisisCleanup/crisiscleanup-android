package com.crisiscleanup.core.mapmarker.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.concurrent.atomic.AtomicBoolean

data class MapViewCameraBounds(
    val bounds: LatLngBounds,
    val durationMs: Int = 500,
    private val initialApply: Boolean = true,
) {
    private val applyToMap = AtomicBoolean(initialApply)

    /**
     * @return true if bounds has yet to be taken (and applied to map) or false otherwise
     */
    fun takeApply(): Boolean = applyToMap.getAndSet(false)
}

val DefaultCoordinates = LatLng(40.272621, -96.012327)
private val swDefault = DefaultCoordinates
private val neDefault = DefaultCoordinates
val MapViewCameraBoundsDefault = MapViewCameraBounds(
    LatLngBounds(swDefault, neDefault),
    0,
    false,
)

data class MapViewCameraZoom(
    val center: LatLng,
    val zoom: Float = 8f,
    val durationMs: Int = 500,
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
