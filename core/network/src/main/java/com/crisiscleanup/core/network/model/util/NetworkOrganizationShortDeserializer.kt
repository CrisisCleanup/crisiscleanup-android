package com.crisiscleanup.core.network.model.util

import com.crisiscleanup.core.network.model.NetworkOrganizationShort
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long

class NetworkOrganizationShortDeserializer : KSerializer<NetworkOrganizationShort?> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.crisiscleanup.NetworkOrganizationShort")

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: NetworkOrganizationShort?) {
        throw SerializationException("Not for deserializing")
    }

    override fun deserialize(decoder: Decoder): NetworkOrganizationShort? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("NetworkOrganizationShortDeserializer only supports JSON decoding")

        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonNull -> null
            is JsonPrimitive -> NetworkOrganizationShort(element.long, "")
            is JsonObject -> {
                jsonDecoder.json.decodeFromJsonElement(
                    NetworkOrganizationShort.serializer(),
                    element,
                )
            }

            else -> throw SerializationException("Unexpected JSON for NetworkOrganizationShort: $element")
        }
    }
}
