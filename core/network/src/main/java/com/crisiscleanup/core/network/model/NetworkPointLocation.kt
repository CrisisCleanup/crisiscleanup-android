package com.crisiscleanup.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkPointLocation(
    val point: NetworkLocationCoordinates,
    val type: String = "point",
)

@Serializable
data class NetworkLocationCoordinates(
    val coordinates: List<Double>,
)
