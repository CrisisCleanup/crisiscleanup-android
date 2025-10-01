package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    "incident_claim_thresholds",
    primaryKeys = ["user_id", "incident_id"],
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["incident_id"],
        ),
    ],
)
data class IncidentClaimThresholdEntity(
    @ColumnInfo("user_id")
    val userId: Long,
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("user_claim_count")
    val userClaimCount: Int,
    @ColumnInfo("user_close_ratio")
    val userCloseRatio: Float,
)
