package com.crisiscleanup.core.commoncase.model

import com.google.android.gms.maps.model.LatLng

data class CoordinateBounds(
    val southWest: LatLng,
    val northEast: LatLng,
)

val CoordinateBoundsDefault = CoordinateBounds(
    LatLng(0.0, 0.0),
    LatLng(0.0, 0.0),
)
