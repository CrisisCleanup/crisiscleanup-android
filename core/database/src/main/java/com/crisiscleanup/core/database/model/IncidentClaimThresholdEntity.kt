package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    "incident_claim_thresholds",
    primaryKeys = ["incident_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class IncidentClaimThresholdEntity(
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("user_id")
    val userId: Long,
    @ColumnInfo("user_claim_count")
    val userClaimCount: Int,
    @ColumnInfo("user_close_ratio")
    val userCloseRatio: Double,
)
