package com.crisiscleanup.core.network.model.util

import com.crisiscleanup.core.network.model.DynamicValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

class DynamicValueSerializer : KSerializer<DynamicValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DynamicValue")

    override fun serialize(encoder: Encoder, value: DynamicValue) {
        if (value.isBoolean) {
            encoder.encodeBoolean(value.valueBoolean)
        } else {
            encoder.encodeString(value.valueString)
        }
    }

    override fun deserialize(decoder: Decoder): DynamicValue {
        when (val json = (decoder as JsonDecoder).decodeJsonElement()) {
            is JsonPrimitive -> {
                json.booleanOrNull?.let {
                    return DynamicValue("", true, it)
                }
                json.contentOrNull?.let {
                    return DynamicValue(it)
                }
                throw Exception("DynamicValue primitive not yet supported")
            }
            else -> throw Exception("DynamicValue type not recognized")
        }
    }
}
