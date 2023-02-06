package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.IterableStringSerializer
import kotlinx.serialization.Serializable

class ExpiredTokenException : Exception("Auth token is expired")

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
        fun tryThrowException(errors: Collection<NetworkCrisisCleanupApiError>?) {
            errors?.let {
                tryGetException(it)?.let { exception -> throw exception }
            }
        }

        fun tryGetException(errors: Collection<NetworkCrisisCleanupApiError>): Exception? {
            if (errors.isNotEmpty()) {
                var exception: Exception? = null
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
