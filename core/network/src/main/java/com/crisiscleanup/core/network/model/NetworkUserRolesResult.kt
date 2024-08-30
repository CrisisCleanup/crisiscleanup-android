package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkUserRolesResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkUserRole>? = null,
)

@Serializable
data class NetworkUserRole(
    val id: Int,
    @SerialName("name_t")
    val nameKey: String,
    @SerialName("description_t")
    val descriptionKey: String,
    val level: Int,
)
