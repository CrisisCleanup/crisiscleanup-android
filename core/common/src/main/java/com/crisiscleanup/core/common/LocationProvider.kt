package com.crisiscleanup.core.common

interface LocationProvider {
    suspend fun getLocation(): Pair<Double, Double>?
}