package com.crisiscleanup.core.network.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class NetworkErrorTest {
    @Test
    fun serialize() {
        val errors = listOf(
            NetworkCrisisCleanupApiError("field", null),
            NetworkCrisisCleanupApiError("field", emptyList()),
            NetworkCrisisCleanupApiError("field", listOf("one")),
            NetworkCrisisCleanupApiError("field", listOf("one", "two")),
        )
        val expecteds = listOf(
            """{"field":"field"}""",
            """{"field":"field","message":[]}""",
            """{"field":"field","message":["one"]}""",
            """{"field":"field","message":["one","two"]}""",
        )
        for (i in expecteds.indices) {
            assertEquals(expecteds[i], Json.encodeToString(errors[i]))
        }
    }

    @Test
    fun deserialize() {
        val expecteds = listOf(
            NetworkCrisisCleanupApiError("field", null),
            NetworkCrisisCleanupApiError("field", null),
            NetworkCrisisCleanupApiError("field", listOf("")),
            NetworkCrisisCleanupApiError("field", listOf("message")),
            NetworkCrisisCleanupApiError("field", emptyList()),
            NetworkCrisisCleanupApiError("field", listOf("one")),
            NetworkCrisisCleanupApiError("field", listOf("one", "two")),
        )
        val jsons = listOf(
            """{"field":"field"}""",
            """{"field":"field","message":null}""",
            """{"field":"field","message":""}""",
            """{"field":"field","message":"message"}""",
            """{"field":"field","message":[]}""",
            """{"field":"field","message":["one"]}""",
            """{"field":"field","message":["one","two"]}""",
        )
        for (i in expecteds.indices) {
            val expected = expecteds[i]
            val actual = Json.decodeFromString<NetworkCrisisCleanupApiError>(jsons[i])
            assertEquals(expected, actual)
        }
    }
}