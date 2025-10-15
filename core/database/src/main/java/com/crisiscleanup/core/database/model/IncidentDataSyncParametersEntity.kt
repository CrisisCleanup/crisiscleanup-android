package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(
    "incident_data_sync_parameters",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class IncidentDataSyncParametersEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("updated_before")
    val updatedBefore: Instant,
    @ColumnInfo("updated_after")
    val updatedAfter: Instant,
    @ColumnInfo("full_updated_before")
    val additionalUpdatedBefore: Instant,
    @ColumnInfo("full_updated_after")
    val additionalUpdatedAfter: Instant,
    @ColumnInfo("bounded_region")
    val boundedRegion: String,
    @ColumnInfo("bounded_synced_at")
    val boundedSyncedAt: Instant,
)
