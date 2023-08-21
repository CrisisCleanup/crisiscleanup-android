package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkWorkTypeStatusResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkWorkTypeStatusFull>? = null,
)

@Serializable
data class NetworkWorkTypeStatusFull(
    val status: String,
    @SerialName("status_name_t")
    val name: String,
    @SerialName("list_order")
    val listOrder: Int,
    @SerialName("primary_state")
    val primaryState: String,
)
