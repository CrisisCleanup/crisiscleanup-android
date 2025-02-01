package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.IncidentWorksitesFullSyncStatsEntity
import com.crisiscleanup.core.database.model.IncidentWorksitesSecondarySyncStatsEntity
import com.crisiscleanup.core.database.model.PopulatedIncidentSyncStats
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
        """,
    )
    @Deprecated("Rewriting")
    fun getSyncStats(incidentId: Long): WorksiteSyncStatsEntity?

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE worksite_sync_stats
        SET paged_count=:pagedCount
        WHERE incident_id=:incidentId AND sync_start=:syncStart
        """,
    )
    @Deprecated("Rewriting")
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
        """,
    )
    @Deprecated("Rewriting")
    fun updateStatsSuccessful(
        incidentId: Long,
        syncStart: Instant,
        pagedCount: Int,
        successfulSync: Instant?,
        attemptedSync: Instant?,
        attemptedCounter: Int,
        appBuildVersionCode: Long,
    )

    @Transaction
    @Query("SELECT * FROM worksite_sync_stats WHERE incident_id=:incidentId")
    @Deprecated("Rewriting")
    fun getIncidentSyncStats(incidentId: Long): PopulatedIncidentSyncStats?

    @Upsert
    @Deprecated("Rewriting")
    fun upsert(stats: IncidentWorksitesFullSyncStatsEntity)

    @Transaction
    @Query("SELECT COUNT(*) FROM worksite_sync_stats")
    @Deprecated("Rewriting")
    fun getWorksiteSyncStatCount(): Long

    @Upsert
    @Deprecated("Rewriting")
    fun upsertSecondaryStats(stats: IncidentWorksitesSecondarySyncStatsEntity)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE incident_worksites_secondary_sync_stats
        SET paged_count=:pagedCount
        WHERE incident_id=:incidentId AND sync_start=:syncStart
        """,
    )
    @Deprecated("Rewriting")
    fun updateSecondaryStatsPaged(
        incidentId: Long,
        syncStart: Instant,
        pagedCount: Int,
    )

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE incident_worksites_secondary_sync_stats
        SET
            paged_count         =:pagedCount,
            successful_sync     =:successfulSync,
            attempted_sync      =:attemptedSync,
            attempted_counter   =:attemptedCounter,
            app_build_version_code=:appBuildVersionCode
        WHERE incident_id=:incidentId AND sync_start=:syncStart
        """,
    )
    @Deprecated("Rewriting")
    fun updateSecondaryStatsSuccessful(
        incidentId: Long,
        syncStart: Instant,
        pagedCount: Int,
        successfulSync: Instant?,
        attemptedSync: Instant?,
        attemptedCounter: Int,
        appBuildVersionCode: Long,
    )
}
