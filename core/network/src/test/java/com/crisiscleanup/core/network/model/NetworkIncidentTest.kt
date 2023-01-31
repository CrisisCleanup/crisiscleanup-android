package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.fake.fillNetworkIncident
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NetworkIncidentTest {
    private val json = Json { ignoreUnknownKeys = true }

    val expectedIncidents = listOf(
        fillNetworkIncident(
            199, "2021-03-10T02:33:48Z",
            "Pandemic (Fake)", "covid_19_response",
            listOf(NetworkIncidentLocation(2, 73141)),
        ),
        fillNetworkIncident(
            151, "2019-07-22T00:00:00Z",
            "Medium Storm (Fake)", "n_wi_derecho_jul_2019",
            listOf(NetworkIncidentLocation(122, 41898)),
        ),
        fillNetworkIncident(
            200, "2022-07-20T16:28:51Z",
            "Another Tornado (Fake)", "another_tornado",
            listOf(
                NetworkIncidentLocation(1, 73132),
                NetworkIncidentLocation(3, 73145)
            ),
        ),
        fillNetworkIncident(
            158, "2019-09-25T00:00:00Z",
            "Small Tornado (Fake)", "chippewa_dunn_wi_tornado",
            listOf(NetworkIncidentLocation(129, 41905)),
        ),
        fillNetworkIncident(
            60, "2017-08-24T00:00:00Z",
            "Big Hurricane (Fake)", "hurricane_harvey",
            listOf(NetworkIncidentLocation(63, 41823)),
            isArchived = true,
        ),
    )

    @Test
    fun getIncidentsSuccessResult() {
        val contents =
            NetworkAuthResult::class.java.getResource("/getIncidentsSuccess.json")?.readText()!!
        val result = json.decodeFromString<NetworkIncidentsResult>(contents)

        assertNull(result.errors)

        assertEquals(5, result.count)

        val incidents = result.results
        assertNotNull(incidents)
        assertEquals(result.count, incidents.size)
        for (i in incidents.indices) {
            assertEquals(expectedIncidents[i], incidents[i])
        }
    }

    @Test
    fun getIncidentsResultFail() {
        val contents =
            NetworkAuthResult::class.java.getResource("/expiredTokenResult.json")?.readText()!!
        val result = json.decodeFromString<NetworkIncidentsResult>(contents)

        assertNull(result.count)
        assertNull(result.results)

        assertEquals(1, result.errors?.size)
        val firstError = result.errors?.get(0)!!
        assertEquals(
            NetworkCrisisCleanupApiError(
                field = "detail",
                message = listOf("Token has expired.")
            ),
            firstError
        )
    }
}