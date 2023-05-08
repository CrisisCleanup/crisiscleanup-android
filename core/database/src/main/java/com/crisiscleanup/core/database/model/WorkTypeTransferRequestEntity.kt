package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.WorkTypeRequest
import kotlinx.datetime.Instant

@Entity(
    "worksite_work_type_requests",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["worksite_id", "work_type", "by_org"], unique = true),
        Index(value = ["network_id"]),
    ],
)
data class WorkTypeTransferRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    /**
     * Local ID. Use [WorksiteDao.getWorksiteId] to find local ID from incident and network ID.
     */
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    @ColumnInfo("work_type")
    val workType: String,
    val reason: String,
    @ColumnInfo("by_org")
    val byOrg: Long,
    @ColumnInfo("to_org")
    val toOrg: Long,
    @ColumnInfo("created_at")
    val createdAt: Instant,
    @ColumnInfo("approved_at")
    val approvedAt: Instant? = null,
    @ColumnInfo("rejected_at")
    val rejectedAt: Instant? = null,
    @ColumnInfo("approved_rejected_reason")
    val approvedRejectedReason: String = "",
)

fun WorkTypeTransferRequestEntity.asExternalModel() = WorkTypeRequest(
    workType = workType,
    byOrg = byOrg,
    createdAt = createdAt,
    approvedAt = approvedAt,
    rejectedAt = rejectedAt,
    approvedRejectedReason = approvedRejectedReason,
)
