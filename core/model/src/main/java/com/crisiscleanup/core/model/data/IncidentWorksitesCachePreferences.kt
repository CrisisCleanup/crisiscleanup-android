package com.crisiscleanup.core.model.data

data class BoundedRegionParameters(
    val isRegionMyLocation: Boolean = false,
    val regionLatitude: Double = 0.0,
    val regionLongitude: Double = 0.0,
    val regionRadiusMiles: Float = 0f,
)

val boundedRegionParametersNone = BoundedRegionParameters()

data class IncidentWorksitesCachePreferences(
    val isPaused: Boolean,
    val isRegionBounded: Boolean,
    val boundedRegionParameters: BoundedRegionParameters,
) {
    val isAutoCache by lazy {
        !(isPaused || isRegionBounded)
    }
}

val InitialIncidentWorksitesCachePreferences = IncidentWorksitesCachePreferences(
    isPaused = false,
    isRegionBounded = false,
    boundedRegionParameters = boundedRegionParametersNone,
)
