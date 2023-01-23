package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * General error from the API
 */
@Serializable
data class NetworkCrisisCleanupApiError(
    val field: String,
    val message: List<String>? = null,
)

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
    val files: List<NetworkAuthUserFiles>?,
)

@Serializable
class NetworkAuthUserFiles(
    val id: Long,
    val file: Long,
    @SerialName("filename")
    val fileName: String,
    val url: String,
    @SerialName("large_thumbnail_url")
    val largeThumbnailUrl: String?,
    @SerialName("file_type_t")
    val fileTypeT: String,
    @SerialName("mime_content_type")
    val mimeContentType: String,
)