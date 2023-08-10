package com.crisiscleanup.core.model.data

interface LocationAreaBounds {
    fun isInBounds(latitude: Double, longitude: Double): Boolean
}

data class OrganizationLocationAreaBounds(
    val primary: LocationAreaBounds? = null,
    val secondary: LocationAreaBounds? = null,
)

interface LocationBoundsConverter {
    fun convert(location: Location): LocationAreaBounds
}