package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkListsResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkList>? = null,
)

@Serializable
data class NetworkList(
    val id: Long,
    @SerialName("created_by")
    val createdBy: Long?,
    @SerialName("updated_by")
    val updatedBy: Long?,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
    val parent: Long?,
    val name: String,
    val description: String?,
    @SerialName("list_order")
    val listOrder: Long?,
    val tags: String?,
    val model: String,
    @SerialName("object_ids")
    val objectIds: List<Long>?,
    val shared: String,
    val permissions: String,
    val incident: Long?,
    @SerialName("invalidate_at")
    val invalidateAt: Instant?,
)

@Serializable
data class NetworkListResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val list: NetworkList? = null,
)
