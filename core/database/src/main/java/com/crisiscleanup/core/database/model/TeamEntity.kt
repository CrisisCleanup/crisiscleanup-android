package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Index.Order
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    "teams_root",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // Locally created unsynced teams will have a network_id=-1.
        // The local/global UUID keeps these worksites unique within the table.
        Index(value = ["network_id", "local_global_uuid"], unique = true),
        Index(value = ["incident_id", "network_id"]),
        // Locally modified worksites for querying sync queue and showing pending syncs.
        Index(
            value = ["is_local_modified", "local_modified_at"],
            orders = [Order.DESC, Order.DESC],
        ),
    ],
)
data class TeamRootEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("local_modified_at", defaultValue = "0")
    val localModifiedAt: Instant,
    @ColumnInfo("synced_at", defaultValue = "0")
    val syncedAt: Instant,
    @ColumnInfo("local_global_uuid", defaultValue = "")
    val localGlobalUuid: String,
    @ColumnInfo("is_local_modified", defaultValue = "0")
    val isLocalModified: Boolean,
    @ColumnInfo("sync_attempt", defaultValue = "0")
    val syncAttempt: Int,

    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    @ColumnInfo("incident_id")
    val incidentId: Long,
)

@Entity(
    "teams",
    foreignKeys = [
        ForeignKey(
            entity = TeamRootEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["incident_id", "network_id"]),
        Index(value = ["network_id"]),
        Index(value = ["incident_id", "name"]),
    ],
)
data class TeamEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    @ColumnInfo("incident_id")
    val incidentId: Long,

    val name: String,
    val notes: String,
    val color: String,

    @ColumnInfo("case_count")
    val caseCount: Int,
    @ColumnInfo("case_complete_count")
    val completeCount: Int,
)

@Entity(
    "team_to_primary_contact",
    primaryKeys = ["team_id", "contact_id"],
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["team_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["contact_id", "team_id"],
            name = "idx_contact_to_team",
        ),
    ],
)
data class TeamMemberCrossRef(
    @ColumnInfo("team_id")
    val teamId: Long,
    @ColumnInfo("contact_id")
    val contactId: Long,
)

@Entity(
    "team_to_equipment",
    primaryKeys = ["team_id", "equipment_id"],
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["team_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["equipment_id", "team_id"],
            name = "idx_equipment_to_team",
        ),
    ],
)
data class TeamEquipmentCrossRef(
    @ColumnInfo("team_id")
    val teamId: Long,
    @ColumnInfo("equipment_id")
    val equipmentId: Long,
)

@Entity(
    "team_work",
    primaryKeys = ["id", "work_type_network_id"],
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["work_type_network_id", "id"],
            name = "idx_work_to_team",
        ),
    ],
)
data class TeamWorkEntity(
    val id: Long,
    @ColumnInfo("work_type_network_id")
    val workTypeNetworkId: Long,
)
