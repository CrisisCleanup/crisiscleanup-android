package com.crisiscleanup.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkLocationsResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkLocation>? = null,
)

@Serializable
data class NetworkLocation(
    val id: Long,
    val geom: LocationGeometry?,
    val poly: LocationPolygon?,
    val point: LocationPoint?,
) {
    val shapeType: String = geom?.type ?: (poly?.type ?: (point?.type ?: ""))

    @Serializable
    data class LocationGeometry(
        val type: String,
        val coordinates: List<List<List<List<Double>>>>,
    ) {
        val condensedCoordinates = coordinates.map { flatten(it[0]) }
    }

    @Serializable
    data class LocationPolygon(
        val type: String,
        val coordinates: List<List<List<Double>>>,
    ) {
        val condensedCoordinates: List<Double> = flatten(coordinates[0])
    }

    @Serializable
    data class LocationPoint(
        val type: String,
        val coordinates: List<Double>,
    )

    companion object {
        internal fun flatten(coordinateSequence: List<List<Double>>): List<Double> {
            val coordinates = mutableListOf<Double>()
            for (latLng in coordinateSequence) {
                coordinates.addAll(latLng)
            }
            return coordinates
        }
    }
}
