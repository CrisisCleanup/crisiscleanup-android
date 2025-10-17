package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkCreateOrgInvitation(
    val model: String,
    @SerialName("created_by")
    val createdBy: Long,
    @SerialName("object_id")
    val organizationId: Long,
)

@Serializable
data class NetworkPersistentInvitationResult(
    val errors: List<NetworkCrisisCleanupApiError>?,
    val invite: NetworkPersistentInvitation?,
)

@Serializable
data class NetworkPersistentInvitation(
    val id: Long,
    val token: String,
    val model: String,
    @SerialName("object_id")
    val objectId: Long,
    @SerialName("requires_approval")
    val requiresApproval: Boolean,
    @SerialName("expires_at")
    val expiresAt: Instant,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("invalidated_at")
    val invalidatedAt: Instant?,
)
