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
    fun getSyncStats(incidentId: Long): WorksiteSyncStatsEntity?

    @Upsert
    fun upsertStats(stats: WorksiteSyncStatsEntity)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE worksite_sync_stats
        SET paged_count=:pagedCount
        WHERE incident_id=:incidentId AND sync_start=:syncStart
        """,
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
        """,
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

    @Transaction
    @Query("SELECT * FROM worksite_sync_stats WHERE incident_id=:incidentId")
    fun getIncidentSyncStats(incidentId: Long): PopulatedIncidentSyncStats?

    @Upsert
    fun upsert(stats: IncidentWorksitesFullSyncStatsEntity)

    @Transaction
    @Query("SELECT COUNT(*) FROM worksite_sync_stats")
    fun getTableCount(): Long

    @Transaction
    @Query("UPDATE incident_worksites_full_sync_stats SET synced_at=NULL WHERE incident_id=:incidentId")
    fun resetFullSync(incidentId: Long)

    @Transaction
    @Query(
        """
        UPDATE incident_worksites_full_sync_stats
        SET center_my_location  =:isMyLocation,
            center_latitude     =:latitude,
            center_longitude    =:longitude
        WHERE incident_id=:incidentId
        """,
    )
    fun setFullSyncCenter(
        incidentId: Long,
        isMyLocation: Boolean,
        latitude: Double,
        longitude: Double,
    )

    @Transaction
    @Query(
        """
        UPDATE incident_worksites_full_sync_stats
        SET query_area_radius=:radius
        WHERE incident_id=:incidentId
        """,
    )
    fun setFullSyncRadius(
        incidentId: Long,
        radius: Double,
    )

    @Upsert
    fun upsertSecondaryStats(stats: IncidentWorksitesSecondarySyncStatsEntity)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE incident_worksites_secondary_sync_stats
        SET paged_count=:pagedCount
        WHERE incident_id=:incidentId AND sync_start=:syncStart
        """,
    )
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
