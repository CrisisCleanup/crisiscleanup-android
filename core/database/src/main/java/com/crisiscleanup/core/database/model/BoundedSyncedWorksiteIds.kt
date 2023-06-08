package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Relation
import kotlinx.datetime.Instant

data class BoundedSyncedWorksiteIds(
    val id: Long,
    @ColumnInfo("network_id")
    val networkId: Long,
    @ColumnInfo("synced_at")
    val syncedAt: Instant,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val formData: List<WorksiteFormDataEntity>,
)

data class SwNeBounds(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
)