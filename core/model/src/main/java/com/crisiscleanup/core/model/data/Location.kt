package com.crisiscleanup.core.model.data

data class Location(
    val id: Long,
    val shape: LocationShape,
    // Should only be defined for Point and Polygon
    val coordinates: List<Double>?,
    // Should only be defined for MultiPolygon
    val multiCoordinates: List<List<Double>>?,
) {
    companion object {
        fun getLocationShape(shapeDescription: String): LocationShape =
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
