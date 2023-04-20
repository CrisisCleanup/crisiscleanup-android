package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.model.data.SyncAttempt
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity(
    "incident_organization_sync_stats",
)
data class IncidentOrganizationSyncStatsEntity(
    @PrimaryKey
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("target_count")
    val targetCount: Int,
    @ColumnInfo("successful_sync")
    val successfulSync: Instant?,
    @ColumnInfo("app_build_version_code", defaultValue = "0")
    val appBuildVersionCode: Long,
)

fun IncidentOrganizationSyncStatsEntity.asExternalModel() = IncidentDataSyncStats(
    incidentId = incidentId,
    syncStart = Clock.System.now(),
    dataCount = targetCount,
    pagedCount = targetCount,
    syncAttempt = SyncAttempt(
        successfulSync?.epochSeconds ?: 0,
        0,
        0,
    ),
    appBuildVersionCode = appBuildVersionCode,
)

fun IncidentDataSyncStats.asOrganizationSyncStatsEntity() = IncidentOrganizationSyncStatsEntity(
    incidentId = incidentId,
    targetCount = dataCount,
    successfulSync = if (syncAttempt.successfulSeconds <= 0) null
    else Instant.fromEpochSeconds(syncAttempt.successfulSeconds),
    appBuildVersionCode = appBuildVersionCode,
)