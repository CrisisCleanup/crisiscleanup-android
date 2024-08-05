package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import kotlinx.datetime.Instant

data class PopulatedLocalModifiedAt(
    @ColumnInfo("id")
    val id: Long,
    @ColumnInfo("network_id")
    val networkId: Long,
    @ColumnInfo("local_modified_at")
    val localModifiedAt: Instant,
    @ColumnInfo("is_local_modified")
    val isLocallyModified: Boolean,
)
