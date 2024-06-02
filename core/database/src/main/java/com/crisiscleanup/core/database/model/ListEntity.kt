package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Index.Order
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    "lists",
    indices = [
        // Locally created unsynced lists have a network_id=-1.
        // The local/global UUID keeps these rows unique within the table.
        Index(value = ["network_id", "local_global_uuid"], unique = true),
        Index(
            value = ["incident_id", "updated_at"],
            orders = [Order.DESC, Order.DESC],
        ),
        Index(
            value = ["updated_at"],
            orders = [Order.DESC],
        ),
        Index(value = ["model"]),
        Index(value = ["parent", "list_order"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
)
data class ListEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    @ColumnInfo("local_global_uuid", defaultValue = "")
    val localGlobalUuid: String,
    @ColumnInfo("created_by")
    val createdBy: Long?,
    @ColumnInfo("updated_by")
    val updatedBy: Long?,
    @ColumnInfo("created_at")
    val createdAt: Instant,
    @ColumnInfo("updated_at")
    val updatedAt: Instant,
    val parent: Long?,
    val name: String,
    val description: String?,
    @ColumnInfo("list_order")
    val listOrder: Long?,
    val tags: String?,
    val model: String,
    @ColumnInfo("object_ids", defaultValue = "")
    val objectIds: String,
    val shared: String,
    val permissions: String,
    @ColumnInfo("incident_id")
    val incidentId: Long?,
)