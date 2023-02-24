package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.WorksitesSyncStats
import kotlinx.datetime.Instant

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
    val appBuildVersionCode: Int,
)

fun WorksiteSyncStatsEntity.asExternalModel() = WorksitesSyncStats(
    incidentId = incidentId,
    syncStart = syncStart,
    worksitesCount = targetCount,
    pagedCount = pagedCount,
    syncAttempt = SyncAttempt(
        successfulSync?.epochSeconds ?: 0,
        attemptedSync?.epochSeconds ?: 0,
        attemptedCounter,
    ),
    appBuildVersionCode = appBuildVersionCode,
)

fun WorksitesSyncStats.asEntity() = WorksiteSyncStatsEntity(
    incidentId = incidentId,
    syncStart = syncStart,
    targetCount = worksitesCount,
    pagedCount = pagedCount,
    successfulSync = if (syncAttempt.successfulSeconds > 0) Instant.fromEpochSeconds(syncAttempt.successfulSeconds) else null,
    attemptedSync = if (syncAttempt.attemptedSeconds > 0) Instant.fromEpochSeconds(syncAttempt.attemptedSeconds) else null,
    attemptedCounter = syncAttempt.attemptedCounter,
    appBuildVersionCode = appBuildVersionCode,
)