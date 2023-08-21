package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkAuthPayload(
    val email: String,
    val password: String,
)

@Serializable
data class NetworkAuthResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("user_claims")
    val claims: NetworkAuthUserClaims? = null,
    val organizations: NetworkAuthOrganization? = null,
)

@Serializable
data class NetworkAuthUserClaims(
    // UPDATE NetworkAuthTest in conjunction with changes here
    val id: Long,
    val email: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val files: List<NetworkFile>?,
)

@Serializable
data class NetworkAuthOrganization(
    val id: Long,
    val name: String,
    @SerialName("is_active")
    val isActive: Boolean,
)

@Serializable
data class NetworkOauthPayload(
    val username: String,
    val password: String,
)

@Serializable
data class NetworkRefreshToken(
    @SerialName("refresh_token")
    val refreshToken: String,
)

@Serializable
data class NetworkOauthResult(
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Int,
)
