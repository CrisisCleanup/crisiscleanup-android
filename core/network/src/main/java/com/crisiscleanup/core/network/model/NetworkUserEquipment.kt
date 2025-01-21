package com.crisiscleanup.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkUserEquipmentListResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkUserEquipment>? = null,
)

@Serializable
data class NetworkUserEquipment(
    val id: Long,
    val equipment: Int,
    val quantity: Int,
    val user: Long,
)
