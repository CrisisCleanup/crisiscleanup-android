package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkInvitationRequest(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val email: String,
    val title: String,
    val password1: String,
    val password2: String,
    val mobile: String,
    @SerialName("requested_to")
    val requestedTo: String,
    @SerialName("primary_language")
    val primaryLanguage: Long,
)

@Serializable
data class NetworkAcceptedInvitationRequest(
    val id: Long,
    @SerialName("requested_to")
    val requestedTo: String,
    @SerialName("requested_to_organization")
    val requestedOrganization: String,
)

@Serializable
data class NetworkAcceptCodeInvite(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val email: String,
    val title: String,
    val password: String,
    val mobile: String,
    @SerialName("invitation_token")
    val invitationToken: String,
    @SerialName("primary_language")
    val primaryLanguage: Long,
)

@Serializable
data class NetworkAcceptPersistentInvite(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val email: String,
    val title: String,
    val password: String,
    val mobile: String,
    val token: String,
)

@Serializable
data class NetworkAcceptedPersistentInvite(
    val detail: String,
)

@Serializable
data class NetworkOrganizationInvite(
    @SerialName("invitee_email")
    val inviteeEmail: String,
    val organization: Long?,
)

@Serializable
data class NetworkOrganizationInviteResult(
    val errors: List<NetworkCrisisCleanupApiError>?,
    val invite: NetworkOrganizationInviteInfo?,
)

@Serializable
data class NetworkOrganizationInviteInfo(
    val id: Long,
    @SerialName("invitee_email")
    val inviteeEmail: String,
    @SerialName("invitation_token")
    val invitationToken: String,
    @SerialName("expires_at")
    val expiresAt: Instant,
    val organization: Long,
    @SerialName("created_at")
    val createdAt: Instant,
)
