package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.IterableStringSerializer
import kotlinx.serialization.Serializable
import okio.IOException

// IO or Retrofit will crash the app
class ExpiredTokenException : IOException("Auth token is expired")
class ServerErrorException(response: okhttp3.Response) :
    IOException("${response.code} ${response.message}")

class CrisisCleanupNetworkException(
    val url: String,
    val statusCode: Int,
    message: String,
    val errors: List<NetworkCrisisCleanupApiError>,
) : IOException(message)

@Serializable
data class NetworkErrors(
    val errors: List<NetworkCrisisCleanupApiError>,
)

/**
 * General error from the API
 */
@Serializable
data class NetworkCrisisCleanupApiError(
    val field: String,
    @Serializable(IterableStringSerializer::class)
    val message: List<String>? = null,
) {
    internal val isExpiredToken = message?.size == 1 && message[0] == "Token has expired."
}

fun Collection<NetworkCrisisCleanupApiError>.tryThrowException() {
    if (isNotEmpty()) {
        val exception =
            if (hasExpiredToken) ExpiredTokenException()
            else Exception(condenseMessages)
        throw exception
    }
}

val Collection<NetworkCrisisCleanupApiError>.hasExpiredToken: Boolean
    get() = any(NetworkCrisisCleanupApiError::isExpiredToken)

val Collection<NetworkCrisisCleanupApiError>.condenseMessages: String
    get() = mapNotNull { it.message?.joinToString(". ") }
        .filter(String::isNotBlank)
        .joinToString("\n")