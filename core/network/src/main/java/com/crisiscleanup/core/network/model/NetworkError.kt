package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.network.model.util.IterableStringSerializer
import kotlinx.serialization.Serializable
import okio.IOException

class ExpiredTokenException : Exception("Auth token is expired")
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
    companion object {
        fun tryThrowException(
            authEventManager: AuthEventManager,
            errors: Collection<NetworkCrisisCleanupApiError>?,
        ) {
            errors?.let {
                tryGetException(errors)?.let {
                    if (it is ExpiredTokenException) {
                        // TODO Move this broadcast into the network layer (and consolidate other broadcasts where possible)
                        authEventManager.onExpiredToken()
                    } else {
                        throw it
                    }
                }
            }
        }

        private fun tryGetException(errors: Collection<NetworkCrisisCleanupApiError>): Exception? {
            if (errors.isNotEmpty()) {
                var exception: Exception? = null
                // Assume expired token will only be a singular exception
                if (errors.size == 1) {
                    exception = errors.first().tryGetException()
                }
                return exception ?: Exception(collapseMessages(errors))
            }
            return null
        }

        private fun collapseMessages(errors: Collection<NetworkCrisisCleanupApiError>) =
            errors.map { it.message?.joinToString(". ") }
                .joinToString("\n")
    }

    private fun tryGetException(): Exception? {
        if (message?.size == 1) {
            if (message[0] == "Token has expired.") {
                return ExpiredTokenException()
            }
        }
        return null
    }
}
