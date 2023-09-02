package com.crisiscleanup.core.addresssearch

import android.location.Geocoder
import com.crisiscleanup.core.addresssearch.model.asLocationAddress
import com.crisiscleanup.core.model.data.LocationAddress
import com.google.android.gms.maps.model.LatLng

internal fun Geocoder.getAddress(coordinates: LatLng): LocationAddress? {
    val results = getFromLocation(coordinates.latitude, coordinates.longitude, 1)
    return results?.firstOrNull()?.asLocationAddress()?.copy(
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
    )
}
