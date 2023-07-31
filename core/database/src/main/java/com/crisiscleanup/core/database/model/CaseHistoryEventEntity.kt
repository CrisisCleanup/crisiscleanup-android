package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    "case_history_events",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["worksite_id", "created_by", "created_at"],
        )
    ],
)
data class CaseHistoryEventEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    @ColumnInfo("created_at")
    val createdAt: Instant,
    @ColumnInfo("created_by")
    val createdBy: Long,
    @ColumnInfo("event_key")
    val eventKey: String,
    @ColumnInfo("past_tense_t")
    val pastTenseT: String,
    @ColumnInfo("actor_location_name")
    val actorLocationName: String,
    @ColumnInfo("recipient_location_name")
    val recipientLocationName: String?,
)

@Entity(
    "case_history_event_attrs",
    foreignKeys = [
        ForeignKey(
            entity = CaseHistoryEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CaseHistoryEventAttrEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("incident_name")
    val incidentName: String,
    @ColumnInfo("patient_case_number")
    val patientCaseNumber: String?,
    @ColumnInfo("patient_id")
    val patientId: Long,
    @ColumnInfo("patient_label_t")
    val patientLabelT: String?,
    @ColumnInfo("patient_location_name")
    val patientLocationName: String?,
    @ColumnInfo("patient_name_t")
    val patientNameT: String?,
    @ColumnInfo("patient_reason_t")
    val patientReasonT: String?,
    @ColumnInfo("patient_status_name_t")
    val patientStatusNameT: String?,
    @ColumnInfo("recipient_case_number")
    val recipientCaseNumber: String?,
    @ColumnInfo("recipient_id")
    val recipientId: Long?,
    @ColumnInfo("recipient_name")
    val recipientName: String?,
    @ColumnInfo("recipient_name_t")
    val recipientNameT: String?,
)