package com.crisiscleanup.core.common

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val ONE_RAD = Math.PI / 180
val Double.radians: Double
    get() = this * ONE_RAD

// https://nssdc.gsfc.nasa.gov/planetary/factsheet/earthfact.html
private const val EARTH_RADIUS_KM = 6_371

/**
 * Conversion from km to miles
 */
val Double.kmToMiles: Double
    get() = this * 0.621371

val Double.milesToMeters: Double
    get() = this * 1609.344

// Formula from https://scikit-learn.org/stable/modules/generated/sklearn.metrics.pairwise.haversine_distances.html#:~:text=The%20Haversine%20(or%20great%20circle,the%20data%20must%20be%202.
/**
 * Distance in km
 *
 * Coordinates are in radians
 *
 * @see radians
 * @see kmToMiles
 */
fun haversineDistance(
    latA: Double,
    lngA: Double,
    latB: Double,
    lngB: Double,
): Double {
    val deltaLat = latA - latB
    val deltaLng = lngA - lngB
    val cosProduct = cos(latA) * cos(latB)
    val a = (sin(deltaLat) * 0.5).pow(2) + cosProduct * (sin(deltaLng) * 0.5).pow(2)
    val c = 2 * asin(sqrt(a).coerceIn(-1.0, 1.0))
    return EARTH_RADIUS_KM * c
}
