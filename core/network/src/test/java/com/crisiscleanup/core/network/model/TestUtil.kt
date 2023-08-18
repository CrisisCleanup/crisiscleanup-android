package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object TestUtil {
    val json = Json { ignoreUnknownKeys = true }

    fun loadFile(filePath: String) =
        TestUtil::class.java.getResource(filePath)?.readText()!!

    inline fun <reified T> decodeResource(filePath: String) =
        json.decodeFromString<T>(loadFile(filePath))
}

internal fun fillNetworkIncident(
    id: Long,
    startAt: String,
    name: String,
    shortName: String,
    incidentType: String,
    locations: List<NetworkIncidentLocation>,
    activePhone: List<String>? = null,
    turnOnRelease: Boolean = false,
    isArchived: Boolean = false,
) = NetworkIncident(
    id,
    Instant.parse(startAt),
    name, shortName,
    locations,
    incidentType,
    activePhone,
    turnOnRelease,
    isArchived,
)

internal fun fillNetworkLocation(
    id: Long,
    geom: List<List<List<List<Double>>>>? = null,
    poly: List<List<List<Double>>>? = null,
    point: List<Double>? = null,
) = NetworkLocation(
    id = id,
    geom = if (geom == null) {
        null
    } else {
        NetworkLocation.LocationGeometry(
            type = "MultiPolygon",
            coordinates = geom,
        )
    },
    poly = if (poly == null) {
        null
    } else {
        NetworkLocation.LocationPolygon(
            type = "Polygon",
            coordinates = poly,
        )
    },
    point = if (point == null) {
        null
    } else {
        NetworkLocation.LocationPoint(
            type = "Point",
            coordinates = point,
        )
    },
)

internal fun splitToTwos(coordinates: List<Double>): List<List<Double>> {
    val twosList = mutableListOf<List<Double>>()
    for (i in coordinates.indices step 2) {
        twosList.add(listOf(coordinates[i], coordinates[i + 1]))
    }
    return twosList
}
