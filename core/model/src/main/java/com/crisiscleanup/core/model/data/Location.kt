package com.crisiscleanup.core.model.data

data class Location(
    val id: Long,
    val shapeLiteral: String,
    // Should only be defined for Point and Polygon
    val coordinates: List<Double>?,
    // Should only be defined for MultiPolygon
    val multiCoordinates: List<List<Double>>?,
) {
    val shape: LocationShape = getLocationShape(shapeLiteral)

    companion object {
        private fun getLocationShape(shapeDescription: String): LocationShape =
            when (shapeDescription.lowercase()) {
                "point" -> LocationShape.Point
                "polygon" -> LocationShape.Polygon
                "multipolygon" -> LocationShape.MultiPolygon
                else -> LocationShape.Unknown
            }
    }
}

enum class LocationShape {
    Unknown,
    Point,
    Polygon,
    MultiPolygon,
}

interface IncidentLocationBounder {
    suspend fun isInBounds(
        incidentId: Long,
        latitude: Double,
        longitude: Double,
    ): Boolean

    suspend fun getBoundsCenter(incidentId: Long): Pair<Double, Double>?
}
