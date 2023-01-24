package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.PopulatedIncident
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Transaction
    @Query(
        """
    SELECT *
    FROM incidents
    ORDER BY start_at DESC
    LIMIT :count
    """
    )
    fun getIncidents(count: Int = 3): Flow<List<PopulatedIncident>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreIncidents(incidents: List<IncidentEntity>): List<Long>

    @Upsert
    suspend fun upsertIncidents(incidents: List<IncidentEntity>)
}
