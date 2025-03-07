package com.crisiscleanup.core.common

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface LocationProvider {
    /**
     * Latitude, longitude
     */
    val coordinates: Pair<Double, Double>?

    /**
     * Latitude, longitude
     */
    suspend fun getLocation(timeout: Duration = 5.seconds): Pair<Double, Double>?
}
