package com.crisiscleanup.feature.cases.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.concurrent.atomic.AtomicBoolean

data class MapViewCameraBounds(
    val bounds: LatLngBounds,
    val initialApply: Boolean = true,
) {
    private val applyToMap = AtomicBoolean(initialApply)

    fun takeApply(): Boolean = applyToMap.getAndSet(false)
}

private val latLngDefault = LatLng(40.272621, -96.012327)
val MapViewCameraBoundsDefault = MapViewCameraBounds(
    LatLngBounds(latLngDefault, latLngDefault),
    false,
)