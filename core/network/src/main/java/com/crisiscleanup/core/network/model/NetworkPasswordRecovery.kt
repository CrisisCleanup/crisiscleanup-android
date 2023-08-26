package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkEmailPayload(
    val email: String,
)

@Serializable
data class NetworkMagicLinkResult(
    val detail: String,
)

@Serializable
data class InitiatePasswordResetResult(
    val id: Long,
    @SerialName("expires_at")
    val expiresAt: Instant,
    @SerialName("is_expired")
    val isExpired: Boolean,
    @SerialName("is_valid")
    val isValid: Boolean,
    @SerialName("invalid_message")
    val invalidMessage: String?,
)

@Serializable
data class NetworkPasswordResetPayload(
    val password: String,
    @SerialName("password_reset_token")
    val token: String,
)

@Serializable
data class NetworkPasswordResetResult(
    val status: String,
)
