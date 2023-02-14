package com.crisiscleanup.feature.cases.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.concurrent.atomic.AtomicBoolean

data class MapViewCameraBounds(
    val bounds: LatLngBounds,
    val durationMs: Int = 500,
    val initialApply: Boolean = true,
) {
    private val applyToMap = AtomicBoolean(initialApply)

    /**
     * @return true if bounds has yet to be taken (and applied to map) or false otherwise
     */
    fun takeApply(): Boolean = applyToMap.getAndSet(false)
}

private val swDefault = LatLng(40.272621, -96.012327)
private val neDefault = LatLng(40.282621, -96.002327)
val MapViewCameraBoundsDefault = MapViewCameraBounds(
    LatLngBounds(swDefault, neDefault),
    0,
    false,
)