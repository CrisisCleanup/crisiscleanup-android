package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.dao.LocationEntitySource
import com.crisiscleanup.core.network.model.NetworkLocation

fun List<NetworkLocation>.asEntitySource() = map {
    val multiCoordinates = it.geom?.condensedCoordinates
    val coordinates = it.poly?.condensedCoordinates ?: it.point?.coordinates
    LocationEntitySource(
        id = it.id,
        shapeType = it.shapeType,
        coordinates = if (multiCoordinates == null) coordinates else null,
        multiCoordinates = multiCoordinates,
    )
}
