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
    val errors: List<NetworkCrisisCleanupApiError>? = null,
)

@Serializable
data class NetworkPhonePayload(
    @SerialName("phone_number")
    val phone: String,
)

@Serializable
data class NetworkPhoneCodePayload(
    @SerialName("phone_number")
    val phone: String,
    @SerialName("otp")
    val code: String,
)

@Serializable
data class NetworkPhoneOneTimePasswordResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val accounts: List<OneTimePasswordPhoneAccount>? = null,
    @SerialName("otp_id")
    val otpId: Long? = null,
)

@Serializable
data class OneTimePasswordPhoneAccount(
    val id: Long,
    val email: String,
    @SerialName("organization")
    val organizationName: String,
)

@Serializable
data class NetworkOneTimePasswordPayload(
    @SerialName("user")
    val accountId: Long,
    @SerialName("otp_id")
    val otpId: Long,
)

@Serializable
data class NetworkPhoneCodeResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val message: String?,
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
