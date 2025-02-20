package com.crisiscleanup.core.model.data

data class IncidentWorksitesCachePreferences(
    val isPaused: Boolean,
    val isRegionBounded: Boolean,
    val isRegionMyLocation: Boolean,
    val regionLatitude: Double,
    val regionLongitude: Double,
    val regionRadiusMiles: Float,
) {
    val isAutoCache by lazy {
        !(isPaused || isRegionBounded)
    }
}

val InitialIncidentWorksitesCachePreferences = IncidentWorksitesCachePreferences(
    isPaused = false,
    isRegionBounded = false,
    isRegionMyLocation = false,
    regionLatitude = 0.0,
    regionLongitude = 0.0,
    regionRadiusMiles = 0f,
)
