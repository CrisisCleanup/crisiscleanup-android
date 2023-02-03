package com.crisiscleanup.core.network.fake

import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkIncident
import com.crisiscleanup.core.network.model.NetworkIncidentLocation
import com.crisiscleanup.core.network.model.NetworkIncidentsResult
import com.crisiscleanup.core.network.model.NetworkWorksitesCountResult
import com.crisiscleanup.core.network.model.NetworkWorksitesFullResult
import com.crisiscleanup.core.network.model.NetworkWorksitesShortResult
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val incidents = listOf(
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
    ),
)

private val incidentsResult = NetworkIncidentsResult(
    count = incidents.size,
    results = incidents
)

private val worksitesCountResult = NetworkWorksitesCountResult(count = 10)
private val worksitesResult = NetworkWorksitesFullResult()
private val worksitesShortResult = NetworkWorksitesShortResult()

@Singleton
class FakeNetworkDataSource @Inject constructor() : CrisisCleanupNetworkDataSource {
    override suspend fun getIncidents(
        fields: List<String>,
        limit: Int,
        ordering: String
    ): NetworkIncidentsResult = incidentsResult

    override suspend fun getWorksites(
        incidentId: Long,
        limit: Int,
        offset: Int
    ): NetworkWorksitesFullResult = worksitesResult

    override suspend fun getWorksitesCount(incidentId: Long): NetworkWorksitesCountResult =
        worksitesCountResult

    override suspend fun getWorksitesAll(
        incidentId: Long,
        updatedAtAfter: Instant?,
        updatedAtBefore: Instant?
    ): NetworkWorksitesShortResult = worksitesShortResult
}

internal fun fillNetworkIncident(
    id: Long,
    startAt: String,
    name: String,
    shortName: String,
    locations: List<NetworkIncidentLocation>,
    turnOn: Boolean = false,
//    activePhone: String? = null,
    isArchived: Boolean = false
) = NetworkIncident(
    id,
    Instant.parse(startAt),
    name, shortName,
    locations,
    turnOn,
//    activePhone,
    isArchived
)