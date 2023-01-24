package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class NetworkIncidentTest {
    private val json = Json { ignoreUnknownKeys = true }

    val expectedIncidents = listOf(
        NetworkIncident(
            199,
            Instant.parse("2021-03-10T02:33:48Z"),
            "Pandemic (Fake)", "covid_19_response",
            listOf(NetworkIncidentLocation(2, 73141, 199, Instant.parse("2021-04-05T22:33:08Z"))),
            false, null
        ),
        NetworkIncident(
            151,
            Instant.parse("2019-07-22T00:00:00Z"),
            "Medium Storm (Fake)", "n_wi_derecho_jul_2019",
            listOf(NetworkIncidentLocation(122, 41898, 151, Instant.parse("2020-03-11T04:07:46Z"))),
            false, null
        ),
        NetworkIncident(
            200,
            Instant.parse("2022-07-20T16:28:51Z"),
            "Another Tornado (Fake)", "another_tornado",
            listOf(
                NetworkIncidentLocation(1, 73132, 200, Instant.parse("2020-11-12T02:37:07Z")),
                NetworkIncidentLocation(3, 73145, 200, Instant.parse("2022-07-20T16:26:21Z"))
            ),
            false, null
        ),
        NetworkIncident(
            158,
            Instant.parse("2019-09-25T00:00:00Z"),
            "Small Tornado (Fake)", "chippewa_dunn_wi_tornado",
            listOf(NetworkIncidentLocation(129, 41905, 158, Instant.parse("2020-03-11T04:17:28Z"))),
            false, null
        ),
        NetworkIncident(
            60,
            Instant.parse("2017-08-24T00:00:00Z"),
            "Big Hurricane (Fake)", "hurricane_harvey",
            listOf(NetworkIncidentLocation(63, 41823, 60, Instant.parse("2020-03-10T17:57:07Z"))),
            false, null
        ),
    )

    @Test
    fun networkGetIncidentsSuccessResultDeserialize() {
        val contents =
            NetworkAuthResult::class.java.getResource("/getIncidentsSuccess.json")?.readText()!!
        val result = json.decodeFromString<NetworkIncidentsResult>(contents)

        assertEquals(result.count, 5)

        val incidents = result.results
        assertEquals(result.count, incidents.size)
        for (i in incidents.indices) {
            assertEquals(expectedIncidents[i], incidents[i])
        }
    }
}