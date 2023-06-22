package com.crisiscleanup.feature.cases.map

import com.google.android.gms.maps.model.LatLng

object CoordinateUtil {
    fun getMiddleLongitude(left: Double, right: Double): Double {
        var l = left
        while (l > right) {
            l -= 360
        }

        val longitude = ((right + l) * 0.5) % 360
        return if (longitude < -180) longitude + 360
        else if (longitude > 180) longitude - 360
        else longitude
    }

    fun getMiddleCoordinate(sw: LatLng, ne: LatLng) = LatLng(
        (sw.latitude + ne.latitude) * 0.5,
        getMiddleLongitude(sw.longitude, ne.longitude),
    )

    fun lerpLatitude(from: Double, to: Double, lerp: Double) = from + (to - from) * lerp

    // TODO Write tests
    fun lerpLongitude(from: Double, to: Double, lerp: Double, lerpToWest: Boolean): Double {
        if (lerpToWest) {
            if (to <= from) {
                return from + (to - from) * lerp
            }

            val wrappedTo = to - 360
            val longitude = from + (wrappedTo - from) * lerp
            return if (longitude < -180) longitude + 360 else longitude

        } else {
            if (to >= from) {
                return from + (to - from) * lerp
            }

            val wrappedTo = to + 360
            val longitude = from + (wrappedTo - from) * lerp
            return if (longitude > 180) longitude - 360 else longitude
        }
    }
}