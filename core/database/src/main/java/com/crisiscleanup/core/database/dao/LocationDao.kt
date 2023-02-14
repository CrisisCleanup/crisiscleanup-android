package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.LocationEntity
import com.crisiscleanup.core.database.model.PopulatedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Transaction
    @Query(
        "SELECT * FROM locations WHERE id in (:ids)"
    )
    fun streamLocations(ids: Collection<Long>): Flow<List<PopulatedLocation>>

    @Upsert
    fun upsertLocations(locations: List<LocationEntity>)
}