package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Corresponds to flags/serializers/FlagSerializer
@Serializable
data class NetworkFlag(
    val id: Long,
    val action: String?,
    @SerialName("created_at")
    val createdAt: Instant,
    // TODO This is missing from full
//    @SerialName("flag_id")
//    val flagId: Int,
    @SerialName("is_high_priority")
    val isHighPriority: Boolean?,
    val notes: String?,
    @SerialName("reason_t")
    val reasonT: String?,
    @SerialName("requested_action")
    val requestedAction: String?,
)