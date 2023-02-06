package com.crisiscleanup.core.network.model.util

import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError
import com.crisiscleanup.core.network.model.NetworkIncident
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class IterableStringSerializerTest {
    @Test
    fun serializeNetworkError() {
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
    fun deserializeNetworkError() {
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

    private val testInstant = Instant.fromEpochSeconds(1675700000)

    @Test
    fun serializeNetworkIncident() {
        val incidents = listOf(
            testNetworkIncident(null),
            testNetworkIncident(listOf("")),
            testNetworkIncident(listOf("phone")),
            testNetworkIncident(emptyList()),
            testNetworkIncident(listOf("one")),
            testNetworkIncident(listOf("one", "two")),
        )
        val expecteds = listOf(
            makeExpectedNetworkIncident("null"),
            makeExpectedNetworkIncident("[\"\"]"),
            makeExpectedNetworkIncident("[\"phone\"]"),
            makeExpectedNetworkIncident("[]"),
            makeExpectedNetworkIncident("""["one"]"""),
            makeExpectedNetworkIncident("""["one","two"]"""),
        )
        for (i in expecteds.indices) {
            assertEquals(expecteds[i], Json.encodeToString(incidents[i]))
        }
    }

    @Test
    fun deserializeNetworkIncident() {
        val networkIncidents = listOf(
            makeExpectedNetworkIncident("null"),
            makeExpectedNetworkIncident("\"\""),
            makeExpectedNetworkIncident("\"phone\""),
            makeExpectedNetworkIncident("[]"),
            makeExpectedNetworkIncident("""["one"]"""),
            makeExpectedNetworkIncident("""["one","two"]"""),
        )
        val expecteds = listOf(
            testNetworkIncident(null),
            testNetworkIncident(listOf("")),
            testNetworkIncident(listOf("phone")),
            testNetworkIncident(emptyList()),
            testNetworkIncident(listOf("one")),
            testNetworkIncident(listOf("one", "two")),
        )
        for (i in expecteds.indices) {
            val decoded = Json.decodeFromString<NetworkIncident>(networkIncidents[i])
            assertEquals(expecteds[i], decoded)
        }
    }

    private fun makeExpectedNetworkIncident(expectedPhoneNumber: String?) =
        """{"id":0,"start_at":"2023-02-06T16:13:20Z","name":"","short_name":"","locations":[],"turn_on_release":null""" +
                ",\"active_phone_number\":$expectedPhoneNumber" +
                ""","is_archived":null""" +
                "}"

    private fun testNetworkIncident(phoneNumbers: List<String>?) = NetworkIncident(
        0,
        testInstant,
        "",
        "",
        emptyList(),
        null,
        phoneNumbers,
        null
    )
}
