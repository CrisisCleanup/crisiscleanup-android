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
            199, "Pandemic (Fake)", "covid_19_response",
            listOf(
                NetworkIncidentLocation(
                    2,
                    73141,
                    199,
                    Instant.parse("2021-04-05T22:33:08Z")
                )
            ),
            false, null
        ),
        NetworkIncident(
            151, "Medium Storm (Fake)", "n_wi_derecho_jul_2019",
            listOf(
                NetworkIncidentLocation(
                    122,
                    41898,
                    151,
                    Instant.parse("2020-03-11T04:07:46Z")
                )
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