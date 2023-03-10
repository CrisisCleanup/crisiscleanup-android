package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    "sync_logs",
    [
        Index(
            value = ["log_time"],
            orders = [Index.Order.DESC],
        )
    ]
)
data class SyncLogEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("log_time")
    val logTime: Instant,
    @ColumnInfo("log_type", defaultValue = "")
    val logType: String,
    val message: String,
    @ColumnInfo(defaultValue = "")
    val details: String,
)
