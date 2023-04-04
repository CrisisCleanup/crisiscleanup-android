package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkFlag(
    // Incoming network ID is always defined
    val id: Long?,
    val action: String?,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("is_high_priority")
    val isHighPriority: Boolean?,
    val notes: String?,
    @SerialName("reason_t")
    val reasonT: String,
    @SerialName("requested_action")
    val requestedAction: String?,
)