package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.model.PopulatedIncident
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    // Update IncidentDaoTest in conjunction with below

    @Transaction
    @Query(
        """
    SELECT *
    FROM incidents
    WHERE is_archived==0
    ORDER BY start_at DESC
    """
    )
    fun getIncidents(): Flow<List<PopulatedIncident>>

    @Upsert
    suspend fun upsertIncidents(incidents: List<IncidentEntity>)

    @Upsert
    suspend fun upsertIncidentLocations(locations: List<IncidentLocationEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreIncidentIncidentLocationCrossRefs(
        incidentCrossRefs: List<IncidentIncidentLocationCrossRef>
    )

    @Query(
        """
    UPDATE incidents
    SET is_archived=1
    WHERE id NOT IN(:unarchivedIds)
    """
    )
    suspend fun setExcludedArchived(unarchivedIds: Set<Long>)
}
