package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.WorkType
import kotlinx.datetime.Instant

@Entity(
    "worksites",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // Each worksite must be unique within an incident
        // Newly created unsynced worksites will have a value of -1. The local/global UUID keeps these worksites unique within the table.
        Index(value = ["incident_id", "network_id", "local_global_uuid"], unique = true),
        // Locally modified worksites for querying sync queue and showing pending syncs.
        Index(value = ["is_local_modified", "sync_attempt"]),

        Index(value = ["incident_id", "network_id", "updated_at"]),
        Index(value = ["network_id"]),
        Index(value = ["incident_id", "latitude", "longitude"]),
        Index(value = ["incident_id", "longitude", "latitude"]),
        Index(value = ["incident_id", "network_id", "svi"]),
    ]
)
data class WorksiteEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("sync_uuid")
    val syncUuid: String,
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
    val address: String,
    @ColumnInfo("auto_contact_frequency_t")
    val autoContactFrequencyT: String,
    @ColumnInfo("case_number")
    val caseNumber: String,
    val city: String,
    val county: String,
    // This can be null if full data is queried without short
    @ColumnInfo("created_at")
    val createdAt: Instant? = null,
    @ColumnInfo(defaultValue = "")
    val email: String?,
    @ColumnInfo("favorite_id")
    val favoriteId: Long?,
    @ColumnInfo("key_work_type_type", defaultValue = "")
    val keyWorkTypeType: String,
    val latitude: Float,
    val longitude: Float,
    val name: String,
    val phone1: String,
    @ColumnInfo(defaultValue = "")
    val phone2: String?,
    @ColumnInfo("plus_code", defaultValue = "")
    val plusCode: String?,
    @ColumnInfo("postal_code")
    val postalCode: String,
    @ColumnInfo("reported_by")
    val reportedBy: Long?,
    val state: String,
    val svi: Float?,
    @ColumnInfo(defaultValue = "")
    val what3Words: String?,
    @ColumnInfo("updated_at")
    val updatedAt: Instant,
)

// TODO Flags XR

@Entity(
    "worksite_work_types",
    indices = [
        Index(value = ["network_id"]),
        Index(value = ["worksite_id"]),
    ]
)
data class WorkTypeEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    @ColumnInfo("created_at")
    val createdAt: Instant? = null,
    @ColumnInfo("claimed_by")
    val orgClaim: Long? = null,
    @ColumnInfo("next_recur_at")
    val nextRecurAt: Instant? = null,
    val phase: Int? = null,
    val recur: String? = null,
    val status: String?,
    @ColumnInfo("work_type")
    val workType: String,
)

fun WorkTypeEntity.asExternalModel() = WorkType(
    /**
     * Local ID not the network ID
     */
    id = id,
    createdAt = createdAt,
    orgClaim = orgClaim,
    nextRecurAt = nextRecurAt,
    phase = phase,
    recur = recur,
    status = status,
    workType = workType,
)

@Entity(
    "worksite_to_work_type",
    primaryKeys = ["worksite_id", "work_type_id"],
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_type_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["work_type_id", "worksite_id"],
            name = "idx_work_type_to_worksite",
        ),
    ]
)
data class WorksiteWorkTypeCrossRef(
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    @ColumnInfo("work_type_id")
    val workTypeId: Long,
)

// TODO Events XR
// TODO Files XR
// TODO Form data XR
// TODO Notes XR

// TODO Worksites FTS

// TODO Unsynced views