package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Index.Order
import androidx.room.PrimaryKey

@Entity(
    "cleanup_equipment",
    indices = [
        Index(value = ["name_t"]),
        Index(
            value = ["is_common", "list_order", "name_t"],
            orders = [Order.DESC, Order.ASC, Order.ASC],
        ),
    ],
)
data class EquipmentEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("list_order")
    val listOrder: Long?,
    @ColumnInfo("is_common")
    val isCommon: Boolean,
    @ColumnInfo("selected_count")
    val selectedCount: Int,
    @ColumnInfo("name_t")
    val nameKey: String,
)
