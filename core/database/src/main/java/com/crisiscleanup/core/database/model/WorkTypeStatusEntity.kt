package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    "work_type_statuses",
    indices = [
        Index(value = ["list_order"]),
    ]
)
data class WorkTypeStatusEntity(
    @PrimaryKey
    val status: String,
    val name: String,
    @ColumnInfo("list_order")
    val listOrder: Int,
    @ColumnInfo("primary_state")
    val primaryState: String,
)
