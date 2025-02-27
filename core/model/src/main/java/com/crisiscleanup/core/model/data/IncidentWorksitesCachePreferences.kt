package com.crisiscleanup.core.model.data

const val BOUNDED_REGION_RADIUS_MILES_DEFAULT = 30.0

data class BoundedRegionParameters(
    val isRegionMyLocation: Boolean = false,
    val regionLatitude: Double = 0.0,
    val regionLongitude: Double = 0.0,
    val regionRadiusMiles: Double = 0.0,
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

    val isBoundedNearMe by lazy {
        isRegionBounded && boundedRegionParameters.isRegionMyLocation
    }

    val isBoundedByCoordinates by lazy {
        isRegionBounded && !boundedRegionParameters.isRegionMyLocation
    }
}

val InitialIncidentWorksitesCachePreferences = IncidentWorksitesCachePreferences(
    isPaused = false,
    isRegionBounded = false,
    boundedRegionParameters = boundedRegionParametersNone,
)
