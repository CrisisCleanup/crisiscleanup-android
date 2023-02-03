package com.crisiscleanup.core.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ExpiredTokenException : Exception("Auth token is expired")

/**
 * General error from the API
 */
@Serializable
data class NetworkCrisisCleanupApiError(
    val field: String,
    @Serializable(with = NetworkErrorMessageSerializer::class)
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

object NetworkErrorMessageSerializer : KSerializer<List<String>?> {
    private val listSerializer = ListSerializer(String.Companion.serializer())
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Message", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: List<String>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            listSerializer.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): List<String>? {
        return when (val json = (decoder as JsonDecoder).decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> listOf(json.contentOrNull ?: "")
            else -> (json as JsonArray).map {
                it.jsonPrimitive.contentOrNull ?: ""
            }
        }
    }
}
