package com.crisiscleanup.core.common

interface LocationProvider {
    val coordinates: Pair<Double, Double>?
    suspend fun getLocation(): Pair<Double, Double>?
}