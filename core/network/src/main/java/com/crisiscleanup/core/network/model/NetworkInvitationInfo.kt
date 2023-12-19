package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkInvitationInfoResult(
    val errors: List<NetworkCrisisCleanupApiError>?,
    val count: Int?,
    val invite: NetworkInvitationInfo?,
)

@Serializable
data class NetworkInvitationInfo(
    @SerialName("invitee_email")
    val inviteeEmail: String,
    @SerialName("expires_at")
    val expiresAt: Instant,
    val organization: Long,
    @SerialName("invited_by")
    val inviter: NetworkInviterInfo,
)

@Serializable
data class NetworkInviterInfo(
    val id: Long,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val email: String,
    @SerialName("mobile")
    val phone: String,
)
