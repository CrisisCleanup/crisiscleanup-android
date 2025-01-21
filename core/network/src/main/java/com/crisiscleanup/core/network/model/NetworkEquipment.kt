package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkEquipmentListResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkEquipment>? = null,
)

@Serializable
data class NetworkEquipment(
    val id: Int,
    @SerialName("list_order")
    val listOrder: Int?,
    @SerialName("is_common")
    val isCommon: Boolean,
    @SerialName("selected_count")
    val selectedCount: Int,
    @SerialName("name_t")
    val nameKey: String,
)
