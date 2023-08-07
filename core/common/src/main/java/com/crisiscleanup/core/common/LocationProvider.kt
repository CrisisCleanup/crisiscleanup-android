package com.crisiscleanup.core.common

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

interface LocationProvider {
    /**
     * Latitude, longitude
     */
    val coordinates: Pair<Double, Double>?

    val cachedLocation: StateFlow<Pair<Double, Double>?>
    val cachedLocationTime: StateFlow<Triple<Double, Double, Instant>?>

    var intervalMillis: Long

    /**
     * Latitude, longitude
     */
    suspend fun getLocation(): Pair<Double, Double>?

    fun startObservingLocation()
    fun stopObservingLocation()
}