package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.IncidentWorksitesSyncStatsEntity

@Dao
interface IncidentWorksitesSyncStatsDao {
    @Transaction
    @Query(
        """
        SELECT *
        FROM incident_worksites_sync_stats
        WHERE id==:incidentId
        """,
    )
    fun getSyncStats(incidentId: Long): IncidentWorksitesSyncStatsEntity?

    @Upsert
    fun upsertStats(stats: IncidentWorksitesSyncStatsEntity)
}
