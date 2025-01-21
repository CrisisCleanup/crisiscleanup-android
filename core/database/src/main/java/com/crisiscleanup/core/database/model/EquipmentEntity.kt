package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
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
    val id: Int,
    @ColumnInfo("list_order")
    val listOrder: Int?,
    @ColumnInfo("is_common")
    val isCommon: Boolean,
    @ColumnInfo("selected_count")
    val selectedCount: Int,
    @ColumnInfo("name_t")
    val nameKey: String,
)

@Entity(
    "user_equipment",
    foreignKeys = [
        // No FK to users as caching order is not guaranteed
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id", "equipment_id"]),
        Index(value = ["equipment_id", "user_id"]),
    ],
)
data class UserEquipmentEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("user_id")
    val userId: Long,
    @ColumnInfo("equipment_id")
    val equipmentId: Int,
    val quantity: Int,
)
