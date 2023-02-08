package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import com.crisiscleanup.core.model.data.Location
import com.crisiscleanup.core.model.data.Location.Companion.getLocationShape

data class PopulatedLocation(
    @Embedded
    val entity: LocationEntity,
)

private fun toDoubleList(s: String) = s.split(",")
    .mapNotNull { it.toDoubleOrNull() }
    .map { it }

fun PopulatedLocation.asExternalModel(): Location {
    val sequenceStrings = entity.coordinates.split("\n")
    var coordinates: List<Double>? = null
    var multiCoordinates: List<List<Double>>? = null
    if (sequenceStrings.size > 1) {
        multiCoordinates = sequenceStrings.map { toDoubleList(it) }
    } else {
        coordinates = toDoubleList(entity.coordinates)
    }
    return Location(
        id = entity.id,
        shape = getLocationShape(entity.shapeType),
        coordinates = coordinates,
        multiCoordinates = multiCoordinates,
    )
}