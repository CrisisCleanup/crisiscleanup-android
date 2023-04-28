package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkWorkType(
    // Incoming network ID is always defined
    val id: Long?,
    @Serializable(InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant? = null,
    @SerialName("claimed_by")
    val orgClaim: Long? = null,
    @Serializable(InstantSerializer::class)
    @SerialName("next_recur_at")
    val nextRecurAt: Instant? = null,
    val phase: Int? = null,
    val recur: String? = null,
    val status: String,
    @SerialName("work_type")
    val workType: String,
)

@Serializable
data class NetworkWorkTypeStatus(
    val status: String,
)

@Serializable
data class NetworkWorkTypeTypes(
    @SerialName("work_types")
    val workTypes: Collection<String>,
)
