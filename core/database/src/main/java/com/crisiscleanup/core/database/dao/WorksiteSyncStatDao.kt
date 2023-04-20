package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.WorksiteSyncStatsEntity
import kotlinx.datetime.Instant

@Dao
interface WorksiteSyncStatDao {
    @Transaction
    @Query(
        """
        SELECT *
        FROM worksite_sync_stats
        WHERE incident_id==:incidentId
        """
    )
    fun getSyncStats(incidentId: Long): List<WorksiteSyncStatsEntity>

    @Upsert
    fun upsertStats(stats: WorksiteSyncStatsEntity)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE worksite_sync_stats
        SET paged_count=:pagedCount
        WHERE incident_id=:incidentId AND sync_start=:syncStart
        """
    )
    fun updateStatsPaged(
        incidentId: Long,
        syncStart: Instant,
        pagedCount: Int,
    )

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE worksite_sync_stats
        SET
            paged_count         =:pagedCount,
            successful_sync     =:successfulSync,
            attempted_sync      =:attemptedSync,
            attempted_counter   =:attemptedCounter,
            app_build_version_code=:appBuildVersionCode
        WHERE incident_id=:incidentId AND sync_start=:syncStart
        """
    )
    fun updateStatsSuccessful(
        incidentId: Long,
        syncStart: Instant,
        pagedCount: Int,
        successfulSync: Instant?,
        attemptedSync: Instant?,
        attemptedCounter: Int,
        appBuildVersionCode: Long,
    )
}