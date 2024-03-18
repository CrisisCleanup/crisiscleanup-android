package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.model.data.SyncAttempt
import kotlinx.datetime.Instant

// Should have been named incident_worksites_sync_stats
@Entity(
    "worksite_sync_stats",
)
data class WorksiteSyncStatsEntity(
    @PrimaryKey
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("sync_start", defaultValue = "0")
    val syncStart: Instant,
    @ColumnInfo("target_count")
    val targetCount: Int,
    @ColumnInfo("paged_count", defaultValue = "0")
    val pagedCount: Int,
    @ColumnInfo("successful_sync")
    val successfulSync: Instant?,
    @ColumnInfo("attempted_sync")
    val attemptedSync: Instant?,
    @ColumnInfo("attempted_counter")
    val attemptedCounter: Int,
    @ColumnInfo("app_build_version_code", defaultValue = "0")
    val appBuildVersionCode: Long,
)

@Entity(
    "incident_worksites_full_sync_stats",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteSyncStatsEntity::class,
            parentColumns = ["incident_id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class IncidentWorksitesFullSyncStatsEntity(
    @PrimaryKey
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("synced_at")
    val syncedAt: Instant?,
    @ColumnInfo("center_my_location")
    val isMyLocationCentered: Boolean,
    @ColumnInfo("center_latitude", defaultValue = "999")
    val latitude: Double,
    @ColumnInfo("center_longitude", defaultValue = "999")
    val longitude: Double,
    /**
     * km
     */
    @ColumnInfo("query_area_radius")
    val radius: Double,
)

@Entity(
    "incident_worksites_secondary_sync_stats",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteSyncStatsEntity::class,
            parentColumns = ["incident_id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class IncidentWorksitesSecondarySyncStatsEntity(
    @PrimaryKey
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("sync_start", defaultValue = "0")
    val syncStart: Instant,
    @ColumnInfo("target_count")
    val targetCount: Int,
    @ColumnInfo("paged_count", defaultValue = "0")
    val pagedCount: Int,
    @ColumnInfo("successful_sync")
    val successfulSync: Instant?,
    @ColumnInfo("attempted_sync")
    val attemptedSync: Instant?,
    @ColumnInfo("attempted_counter")
    val attemptedCounter: Int,
    @ColumnInfo("app_build_version_code", defaultValue = "0")
    val appBuildVersionCode: Long,
)

fun WorksiteSyncStatsEntity.asExternalModel() = IncidentDataSyncStats(
    incidentId = incidentId,
    syncStart = syncStart,
    dataCount = targetCount,
    pagedCount = pagedCount,
    syncAttempt = SyncAttempt(
        successfulSync?.epochSeconds ?: 0,
        attemptedSync?.epochSeconds ?: 0,
        attemptedCounter,
    ),
    appBuildVersionCode = appBuildVersionCode,
)

fun IncidentDataSyncStats.asWorksiteSyncStatsEntity() = WorksiteSyncStatsEntity(
    incidentId = incidentId,
    syncStart = syncStart,
    targetCount = dataCount,
    pagedCount = pagedCount,
    successfulSync = if (syncAttempt.successfulSeconds <= 0) {
        null
    } else {
        Instant.fromEpochSeconds(syncAttempt.successfulSeconds)
    },
    attemptedSync = if (syncAttempt.attemptedSeconds <= 0) {
        null
    } else {
        Instant.fromEpochSeconds(syncAttempt.attemptedSeconds)
    },
    attemptedCounter = syncAttempt.attemptedCounter,
    appBuildVersionCode = appBuildVersionCode,
)

fun IncidentDataSyncStats.asSecondaryWorksiteSyncStatsEntity() =
    IncidentWorksitesSecondarySyncStatsEntity(
        incidentId = incidentId,
        syncStart = syncStart,
        targetCount = dataCount,
        pagedCount = pagedCount,
        successfulSync = if (syncAttempt.successfulSeconds <= 0) {
            null
        } else {
            Instant.fromEpochSeconds(syncAttempt.successfulSeconds)
        },
        attemptedSync = if (syncAttempt.attemptedSeconds <= 0) {
            null
        } else {
            Instant.fromEpochSeconds(syncAttempt.attemptedSeconds)
        },
        attemptedCounter = syncAttempt.attemptedCounter,
        appBuildVersionCode = appBuildVersionCode,
    )

fun IncidentWorksitesSecondarySyncStatsEntity.asExternalModel() = IncidentDataSyncStats(
    incidentId = incidentId,
    syncStart = syncStart,
    dataCount = targetCount,
    pagedCount = pagedCount,
    syncAttempt = SyncAttempt(
        successfulSync?.epochSeconds ?: 0,
        attemptedSync?.epochSeconds ?: 0,
        attemptedCounter,
    ),
    appBuildVersionCode = appBuildVersionCode,
)
