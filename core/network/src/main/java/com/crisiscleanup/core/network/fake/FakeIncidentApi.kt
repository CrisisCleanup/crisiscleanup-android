package com.crisiscleanup.core.network.fake

import com.crisiscleanup.core.network.CrisisCleanupIncidentApi
import com.crisiscleanup.core.network.model.NetworkIncident
import com.crisiscleanup.core.network.model.NetworkIncidentLocation
import com.crisiscleanup.core.network.model.NetworkIncidentsResult
import kotlinx.datetime.Instant
import javax.inject.Inject

private val incidents = NetworkIncidentsResult(
    count = 1,
    results = listOf(
        NetworkIncident(
            199,
            Instant.parse("2021-03-10T02:33:48Z"),
            "Pandemic (Fake)", "covid_19_response",
            listOf(NetworkIncidentLocation(2, 73141)),
            false, null
        ),
        NetworkIncident(
            151,
            Instant.parse("2019-07-22T00:00:00Z"),
            "Medium Storm (Fake)", "n_wi_derecho_jul_2019",
            listOf(NetworkIncidentLocation(122, 41898)),
            false, null
        ),
        NetworkIncident(
            200,
            Instant.parse("2022-07-20T16:28:51Z"),
            "Another Tornado (Fake)", "another_tornado",
            listOf(
                NetworkIncidentLocation(1, 73132),
                NetworkIncidentLocation(3, 73145),
            ),
            false, null
        ),
    )
)

class FakeIncidentApi @Inject constructor() : CrisisCleanupIncidentApi {
    override suspend fun getIncidents(
        fields: List<String>,
        limit: Int,
        ordering: String
    ): NetworkIncidentsResult = incidents
}