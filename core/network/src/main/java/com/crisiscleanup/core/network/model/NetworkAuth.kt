package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
data class NetworkCodeAuthResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = null,
)
