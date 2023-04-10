package com.crisiscleanup.core.database.model

import androidx.room.*
import androidx.room.Index.Order
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
        Index(
            value = ["worksite_id", "created_at"],
            orders = [Order.ASC, Order.DESC],
        ),
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
    @ColumnInfo("change_data")
    val changeData: String,
    @ColumnInfo("created_at")
    val createdAt: Instant = Clock.System.now(),
    @ColumnInfo("save_attempt", defaultValue = "0")
    val saveAttempt: Int = 0,
    @ColumnInfo("save_fail_response", defaultValue = "")
    val saveFailResponse: String = "",
    /**
     * @see WorksiteChangeArchiveAction
     */
    @ColumnInfo("archive_action")
    val archiveAction: String = "",
    @ColumnInfo("archived_at")
    val archivedAt: Instant = Instant.fromEpochSeconds(0),
)

@Entity(
    "worksite_change_notice",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteRootEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorksiteChangeEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_change_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(
            value = ["worksite_id", "created_at"],
            orders = [Order.ASC, Order.DESC]
        ),
        Index(value = ["worksite_id", "worksite_change_id"]),
        Index(
            value = ["created_at", "is_acknowledged"],
            orders = [Order.DESC, Order.ASC]
        ),
    ]
)
data class WorksiteChangeNoticeEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    @ColumnInfo("created_at")
    val createdAt: Instant,
    @ColumnInfo("worksite_change_id")
    val changeId: Instant,
    // Can be http status code or internal message code
    val code: Int,
    // When code is insufficient/lacking
    @ColumnInfo("notice_type")
    val noticeType: String,
    val message: String,
    // Serialized details when additional context is available. Determine data type with code/noticeType
    val details: String,
    @ColumnInfo("is_acknowledged", defaultValue = "0")
    val isAcknowledged: Boolean,
)

enum class WorksiteChangeArchiveAction(val literal: String) {
    // Pending sync
    Pending(""),

    // Synced successfully
    Synced("synced"),

    // Sync failed too many times and should not be tried again
    Skipped("skipped"),
}