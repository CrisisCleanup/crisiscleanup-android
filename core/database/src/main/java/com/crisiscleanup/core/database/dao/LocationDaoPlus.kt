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
            .map { source ->
                var coordinates = source.coordinates?.let {
                    it.joinToString(",")
                } ?: ""
                if (coordinates.isEmpty()) {
                    coordinates = source.multiCoordinates?.let {
                        it.joinToString("\n") { l ->
                            l.joinToString(",")
                        }
                    } ?: ""
                }
                LocationEntity(
                    id = source.id,
                    shapeType = source.shapeType,
                    coordinates = coordinates,
                )
            }
            .filter { it.coordinates.isNotEmpty() }
        db.locationDao().upsertLocations(locations)
    }
}

data class LocationEntitySource(
    val id: Long,
    val shapeType: String,
    val coordinates: List<Double>?,
    val multiCoordinates: List<List<Double>>?,
)