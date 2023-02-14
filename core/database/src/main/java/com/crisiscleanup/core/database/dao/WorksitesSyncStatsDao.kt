package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.WorksiteSyncStatsEntity

@Dao
interface WorksitesSyncStatsDao {
    @Transaction
    @Query(
        """
        SELECT *
        FROM worksite_sync_stats
        WHERE incidentId==:incidentId
        """
    )
    fun getSyncStats(incidentId: Long): List<WorksiteSyncStatsEntity>

    @Upsert
    fun upsertStats(stats: WorksiteSyncStatsEntity)
}