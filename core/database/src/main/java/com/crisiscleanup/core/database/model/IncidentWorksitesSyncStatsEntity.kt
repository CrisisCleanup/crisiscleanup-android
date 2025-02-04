package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    "incident_worksites_sync_stats",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class IncidentWorksitesSyncStatsEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("updated_before")
    val updatedBefore: Instant,
    @ColumnInfo("updated_after")
    val updatedAfter: Instant,
    @ColumnInfo("full_updated_before")
    val fullUpdatedBefore: Instant,
    @ColumnInfo("full_updated_after")
    val fullUpdatedAfter: Instant,
    @ColumnInfo("bounded_region")
    val boundedRegion: String,
    @ColumnInfo("bounded_synced_at")
    val boundedSyncedAt: Instant,
    @ColumnInfo("app_build_version_code", defaultValue = "0")
    val appBuildVersionCode: Long,
)
