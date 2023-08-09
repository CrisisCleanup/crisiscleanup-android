package com.crisiscleanup.core.common

interface LocationProvider {
    /**
     * Latitude, longitude
     */
    val coordinates: Pair<Double, Double>?

    /**
     * Latitude, longitude
     */
    suspend fun getLocation(): Pair<Double, Double>?
}