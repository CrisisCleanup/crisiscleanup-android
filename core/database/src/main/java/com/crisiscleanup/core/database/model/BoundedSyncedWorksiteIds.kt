package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import kotlinx.datetime.Instant

data class BoundedSyncedWorksiteIds(
    val id: Long,
    @ColumnInfo("network_id")
    val networkId: Long,
    val phone1: String?,
    @ColumnInfo("synced_at")
    val syncedAt: Instant,
)

data class SwNeBounds(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
)