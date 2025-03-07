package com.crisiscleanup.core.common

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentMapTracker {
    /**
     * latitude, longitude
     */
    val lastLocation: Pair<Double, Double>?

    fun track(latitude: Double, longitude: Double)
}

@Singleton
class AppIncidentMapTracker @Inject constructor() : IncidentMapTracker {
    private val locationCache = AtomicReference<Pair<Double, Double>?>(null)

    override val lastLocation: Pair<Double, Double>?
        get() = locationCache.get()

    override fun track(latitude: Double, longitude: Double) {
        locationCache.set(Pair(latitude, longitude))
    }
}
