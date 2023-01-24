package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.IncidentLocationEntity

@Dao
interface IncidentLocationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreLocations(locations: List<IncidentLocationEntity>): List<Long>

    @Upsert
    suspend fun upsertLocations(locations: List<IncidentLocationEntity>)
}