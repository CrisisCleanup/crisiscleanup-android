package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.IncidentWorksitesSyncStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentWorksitesSyncStatDao {
    @Transaction
    @Query("SELECT * FROM incident_worksites_sync_stats WHERE id=:id")
    fun streamWorksitesSyncStats(id: Long): Flow<IncidentWorksitesSyncStatsEntity?>

    @Transaction
    @Query(
        """
        SELECT *
        FROM incident_worksites_sync_stats
        WHERE id==:incidentId
        """,
    )
    fun getSyncStats(incidentId: Long): IncidentWorksitesSyncStatsEntity?

    @Insert
    fun insertSyncStats(entity: IncidentWorksitesSyncStatsEntity)

    @Upsert
    fun upsertStats(stats: IncidentWorksitesSyncStatsEntity)

    @Transaction
    @Query("DELETE FROM incident_worksites_sync_stats WHERE id=:id")
    fun deleteSyncStats(id: Long)
}
