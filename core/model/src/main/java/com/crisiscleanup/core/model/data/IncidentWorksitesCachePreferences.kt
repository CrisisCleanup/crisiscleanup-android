package com.crisiscleanup.core.model.data

data class IncidentWorksitesCachePreferences(
    val isPaused: Boolean,
    val isRegionBounded: Boolean,
    val regionLatitude: Double,
    val regionLongitude: Double,
    val regionRadiusMiles: Float,
)
