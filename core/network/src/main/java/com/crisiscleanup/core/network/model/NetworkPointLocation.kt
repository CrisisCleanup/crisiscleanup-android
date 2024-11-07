package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkPointLocation(
    val point: NetworkLocationCoordinates,
)

@Serializable
data class NetworkLocationCoordinates(
    val coordinates: List<Double>,
    val type: String,
)

@Serializable
data class NetworkLocationUpdate(
    val user: Long,
    val point: NetworkLocationCoordinates,
    @SerialName("updated_at")
    val updatedAt: Instant,
)
