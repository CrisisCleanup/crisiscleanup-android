package com.crisiscleanup.core.mapmarker.model

import com.crisiscleanup.core.model.data.LocationShape
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil

data class LocationLatLng(
    val id: Long,
    val shape: LocationShape,
    val multiCoordinates: List<List<LatLng>>,
)

data class LocationBounds(
    val locationLatLng: LocationLatLng,
    val multiBounds: List<LatLngBounds?>,
) {
    fun containsLocation(location: LatLng): Boolean {
        locationLatLng.multiCoordinates.forEachIndexed { index, latLngs ->
            multiBounds[index]?.let { latLngBounds ->
                if (latLngBounds.contains(location) &&
                    PolyUtil.containsLocation(location, latLngs, true)
                ) {
                    return true
                }
            }
        }
        return false
    }
}

data class IncidentBounds(
    val locations: Collection<LocationBounds>,
    val bounds: LatLngBounds,
    val center: LatLng = bounds.center,
) {
    fun containsLocation(location: LatLng) =
        bounds.contains(location) && locations.any { it.containsLocation(location) }
}
