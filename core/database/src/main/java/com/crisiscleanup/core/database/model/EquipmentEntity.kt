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
    "user_equipments",
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
        // Locally created unsynced user equipment will have a network_id=-1.
        // The local/global UUID keeps these worksites unique within the table.
        Index(value = ["network_id", "local_global_uuid"], unique = true),
        Index(value = ["user_id", "equipment_id", "is_local_modified"]),
        Index(value = ["equipment_id", "user_id"]),
        Index(value = ["is_local_modified", "network_id"]),
    ],
)
data class UserEquipmentEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("local_global_uuid", defaultValue = "")
    val localGlobalUuid: String,
    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    @ColumnInfo("is_local_modified", defaultValue = "0")
    val isLocalModified: Boolean,

    @ColumnInfo("user_id")
    val userId: Long,
    @ColumnInfo("equipment_id")
    val equipmentId: Int,
    val quantity: Int,
)
