package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.DynamicValueSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyDynamicValuePair(
    @SerialName("field_key")
    val key: String,
    @SerialName("field_value")
    val value: DynamicValue,
)

@Serializable(DynamicValueSerializer::class)
data class DynamicValue(
    val valueString: String,
    val isBoolean: Boolean = false,
    val valueBoolean: Boolean = false,
)