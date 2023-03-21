package com.crisiscleanup.core.addresssearch.util

import com.crisiscleanup.core.addresssearch.model.KeyLocationAddress
import com.crisiscleanup.core.addresssearch.model.toLatLng
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

internal fun List<KeyLocationAddress>.sort(center: LatLng?): List<KeyLocationAddress> {
    var sorted = this
    center?.let { sortCenter ->
        sorted = map {
            val distance = SphericalUtil.computeDistanceBetween(it.address.toLatLng(), sortCenter)
            Pair(it, distance)
        }
            .sortedWith { a, b -> if (a.second - b.second < 0) -1 else 1 }
            .map { it.first }
    }
    return sorted
}
