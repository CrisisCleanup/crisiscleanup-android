package com.crisiscleanup.core.network.fake

import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val incidents = listOf(
    fillNetworkIncident(
        199, "2021-03-10T02:33:48Z",
        "Pandemic (Fake)", "covid_19_response", "virus",
        listOf(NetworkIncidentLocation(2, 73141)),
    ),
    fillNetworkIncident(
        151, "2019-07-22T00:00:00Z",
        "Medium Storm (Fake)", "n_wi_derecho_jul_2019", "wind",
        listOf(NetworkIncidentLocation(122, 41898)),
    ),
    fillNetworkIncident(
        200, "2022-07-20T16:28:51Z",
        "Another Tornado (Fake)", "another_tornado", "tornado",
        listOf(
            NetworkIncidentLocation(1, 73132),
            NetworkIncidentLocation(3, 73145)
        ),
    ),
    fillNetworkIncident(
        158, "2019-09-25T00:00:00Z",
        "Small Tornado (Fake)", "chippewa_dunn_wi_tornado",
        "tornado",
        listOf(NetworkIncidentLocation(129, 41905)),
    ),
    fillNetworkIncident(
        60, "2017-08-24T00:00:00Z",
        "Big Hurricane (Fake)", "hurricane_harvey", "hurricane",
        listOf(NetworkIncidentLocation(63, 41823)),
    ),
)

private val incidentsResult = NetworkIncidentsResult(
    count = incidents.size,
    results = incidents
)

private val locations = listOf(
    fillNetworkLocation(
        1, poly = listOf(
            splitToTwos(
                listOf(
                    -85.748032,
                    31.619180999999998,
                    -85.745435,
                    31.618897999999998,
                    -85.742651,
                    31.621258999999995,
                    -85.74174,
                    31.619403,
                    -85.739813,
                    31.62181,
                    -85.73992100000001,
                    31.623321999999998,
                    -85.73693200000001,
                    31.623691,
                    -85.731172,
                    31.629939999999998
                )
            )
        )
    ),
    fillNetworkLocation(
        2, geom = listOf(
            listOf(
                splitToTwos(
                    listOf(
                        -105.99888629510694,
                        31.39394035670263,
                        -106.21328529530662,
                        31.478246356781145,
                        -106.38358129546522,
                        31.73387235701922,
                        -106.53951429561045,
                        31.786305357068045,
                        -106.61498629568072,
                        31.81783435709741,
                        -106.61612329568179,
                        31.84474035712247
                    )
                )
            ),
            listOf(
                splitToTwos(
                    listOf(
                        -94.91362828478299,
                        29.257810354713204,
                        -94.76757528464697,
                        29.342686354792246,
                        -94.74860028462929,
                        29.319727354770865,
                        -95.1056212849618,
                        29.097200354563626,
                        -94.91362828478299,
                        29.257810354713204
                    )
                )
            ),
        )
    ),
    fillNetworkLocation(
        3, point = listOf(-73.2886979, 38.3069709)
    ),
)

private val incidentLocationsResult = NetworkLocationsResult(
    count = locations.size,
    results = locations,
)

private val worksitesCountResult = NetworkCountResult(count = 10)
private val worksitesResult = NetworkWorksitesFullResult()
private val worksitesShortResult = NetworkWorksitesShortResult()

@Singleton
class FakeNetworkDataSource @Inject constructor() : CrisisCleanupNetworkDataSource {
    override suspend fun getIncidents(
        fields: List<String>,
        limit: Int,
        ordering: String,
        after: Instant?,
    ) = incidentsResult

    override suspend fun getIncidentLocations(locationIds: List<Long>) =
        incidentLocationsResult

    override suspend fun getIncident(id: Long, fields: List<String>) =
        NetworkIncidentResult(incident = incidents[0])

    override suspend fun getWorksites(incidentId: Long, limit: Int, offset: Int) = worksitesResult

    override suspend fun getWorksites(worksiteIds: Collection<Long>) = worksitesResult

    override suspend fun getWorksitesCount(incidentId: Long, updatedAtAfter: Instant?) =
        worksitesCountResult

    override suspend fun getWorksitesAll(
        incidentId: Long,
        updatedAtAfter: Instant?,
        updatedAtBefore: Instant?
    ) = worksitesShortResult

    override suspend fun getWorksitesPage(
        incidentId: Long,
        updatedAtAfter: Instant?,
        pageCount: Int,
        pageOffset: Int?,
        latitude: Double?,
        longitude: Double?
    ) = worksitesShortResult

    override suspend fun getLanguages() =
        NetworkLanguagesResult(
            results = listOf(NetworkLanguageDescription("en-US", "English US"))
        )

    override suspend fun getLanguageTranslations(key: String) = NetworkLanguageTranslationResult(
        translation = NetworkLanguageTranslation(
            "en-US", "English US", emptyMap()
        )
    )

    override suspend fun getLocalizationCount(after: Instant) = NetworkCountResult(count = 0)
}

internal fun fillNetworkIncident(
    id: Long,
    startAt: String,
    name: String,
    shortName: String,
    incidentType: String,
    locations: List<NetworkIncidentLocation>,
    activePhone: List<String>? = null,
    isArchived: Boolean = false
) = NetworkIncident(
    id,
    Instant.parse(startAt),
    name, shortName,
    locations,
    incidentType,
    activePhone,
    isArchived,
)

internal fun fillNetworkLocation(
    id: Long,
    geom: List<List<List<List<Double>>>>? = null,
    poly: List<List<List<Double>>>? = null,
    point: List<Double>? = null,
) = NetworkLocation(
    id = id,
    geom = if (geom == null) null else NetworkLocation.LocationGeometry(
        type = "MultiPolygon",
        coordinates = geom,
    ),
    poly = if (poly == null) null else NetworkLocation.LocationPolygon(
        type = "Polygon",
        coordinates = poly,
    ),
    point = if (point == null) null else NetworkLocation.LocationPoint(
        type = "Point",
        coordinates = point,
    ),
)

internal fun splitToTwos(coordinates: List<Double>): List<List<Double>> {
    val twosList = mutableListOf<List<Double>>()
    for (i in coordinates.indices step 2) {
        twosList.add(listOf(coordinates[i], coordinates[i + 1]))
    }
    return twosList
}