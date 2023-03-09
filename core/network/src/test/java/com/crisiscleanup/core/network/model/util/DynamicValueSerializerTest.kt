package com.crisiscleanup.core.network.model.util

import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.core.network.model.KeyDynamicValuePair
import com.crisiscleanup.core.network.model.TestUtil
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test
import kotlin.test.assertEquals

class DynamicValueSerializerTest {
    private fun serializedEntry(key: String, value: String) =
        """{"field_key":"$key","field_value":$value}"""

    @Test
    fun serialize() {
        val formData = listOf(
            KeyDynamicValuePair(
                key = "string-key",
                value = DynamicValue("string-value"),
            ),
            KeyDynamicValuePair(
                key = "boolean-key",
                value = DynamicValue("", isBoolean = true, true),
            ),
            KeyDynamicValuePair(
                key = "boolean-false-key",
                value = DynamicValue("", isBoolean = true, false),
            ),
        )

        val serialized = TestUtil.json.encodeToString(formData)
        val expected =
            """[${serializedEntry("string-key", """"string-value"""")},${
                serializedEntry("boolean-key", "true")
            },${
                serializedEntry("boolean-false-key", "false")
            }]"""
        assertEquals(expected, serialized)
    }

    @Test
    fun deserialize() {
        val deserialized = TestUtil.json.decodeFromString<List<KeyDynamicValuePair>>(
            """[${serializedEntry("boolean-key", "true")},${
                serializedEntry("string-key", """"string-value"""")
            },${serializedEntry("boolean-false-key", "false")}]"""
        )
        val expected = listOf(
            KeyDynamicValuePair(
                key = "boolean-key",
                value = DynamicValue("", isBoolean = true, true),
            ),
            KeyDynamicValuePair(
                key = "string-key",
                value = DynamicValue("string-value"),
            ),
            KeyDynamicValuePair(
                key = "boolean-false-key",
                value = DynamicValue("", isBoolean = true, false),
            ),
        )
        assertEquals(expected, deserialized)
    }
}