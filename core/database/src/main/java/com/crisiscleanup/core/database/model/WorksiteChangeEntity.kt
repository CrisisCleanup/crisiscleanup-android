package com.crisiscleanup.core.database.model

import androidx.room.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity(
    "worksite_changes",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteRootEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["worksite_id", "created_at"]),
    ]
)
data class WorksiteChangeEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("app_version")
    val appVersion: Long,
    @ColumnInfo("organization_id")
    val organizationId: Long,
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    // Only applies to worksite core data. Attaching data can check on keys or content.
    @ColumnInfo("sync_uuid", defaultValue = "")
    val syncUuid: String,
    @ColumnInfo("change_model_version")
    val changeModelVersion: Int,
    @ColumnInfo("change_data")
    val changeData: String,
    @ColumnInfo("created_at")
    val createdAt: Instant = Clock.System.now(),
    @ColumnInfo("save_attempt", defaultValue = "0")
    val saveAttempt: Int = 0,
    /**
     * @see com.crisiscleanup.core.model.data.WorksiteChangeArchiveAction
     */
    @ColumnInfo("archive_action")
    val archiveAction: String = "",
    @ColumnInfo("save_attempt_at", defaultValue = "0")
    val saveAttemptAt: Instant = Instant.fromEpochSeconds(0),
)