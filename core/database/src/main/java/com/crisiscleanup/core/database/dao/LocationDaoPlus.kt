package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.LocationEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    fun saveLocations(
        locationsSource: List<LocationEntitySource>,
    ) {
        val locations = locationsSource
            .mapNotNull { source ->
                // Assumes coordinates and multiCoordinates are lng-lat ordered pairs
                var coordinates = source.coordinates?.joinToString(",") ?: ""
                if (coordinates.isEmpty()) {
                    coordinates = source.multiCoordinates?.let {
                        it.joinToString("\n") { l ->
                            l.joinToString(",")
                        }
                    } ?: ""
                }
                if (coordinates.isEmpty()) null
                else LocationEntity(
                    id = source.id,
                    shapeType = source.shapeType,
                    coordinates = coordinates,
                )
            }
        db.locationDao().upsertLocations(locations)
    }
}

data class LocationEntitySource(
    val id: Long,
    val shapeType: String,
    val coordinates: List<Double>?,
    val multiCoordinates: List<List<Double>>?,
)