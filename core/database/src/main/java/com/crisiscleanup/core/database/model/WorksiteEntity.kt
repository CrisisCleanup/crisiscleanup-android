package com.crisiscleanup.core.database.model

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Index.Order
import androidx.room.PrimaryKey
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteNote
import kotlinx.datetime.Instant

// Changes below should update WORKSITES_STABLE_MODEL_BUILD_VERSION in core.network

@Entity(
    "worksites_root",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // Locally created unsynced worksites will have a network_id=-1.
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
data class WorksiteRootEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("sync_uuid", defaultValue = "")
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
)

@Entity(
    "worksites",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteRootEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["incident_id", "network_id"]),
        Index(value = ["network_id"]),
        Index(value = ["incident_id", "latitude", "longitude"]),
        Index(value = ["incident_id", "longitude", "latitude"]),
        Index(value = ["incident_id", "svi"]),
        Index(value = ["incident_id", "updated_at"]),
        Index(value = ["incident_id", "created_at"]),
        Index(value = ["incident_id", "case_number"]),
        Index(value = ["incident_id", "case_number_order", "case_number"]),
        Index(value = ["incident_id", "name", "county", "city", "case_number_order", "case_number"]),
        Index(value = ["incident_id", "city", "name", "case_number_order", "case_number"]),
        Index(value = ["incident_id", "county", "name", "case_number_order", "case_number"]),
    ],
)
data class WorksiteEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    @ColumnInfo("incident_id")
    val incidentId: Long,
    val address: String,
    @ColumnInfo("auto_contact_frequency_t")
    val autoContactFrequencyT: String?,
    @ColumnInfo("case_number")
    val caseNumber: String,
    @ColumnInfo("case_number_order", defaultValue = "0")
    val caseNumberOrder: Long,
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
    @ColumnInfo("key_work_type_org")
    val keyWorkTypeOrgClaim: Long?,
    @ColumnInfo("key_work_type_status", defaultValue = "")
    val keyWorkTypeStatus: String,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val phone1: String?,
    @ColumnInfo("phone1_notes", defaultValue = "")
    val phone1Notes: String?,
    @ColumnInfo(defaultValue = "")
    val phone2: String?,
    @ColumnInfo("phone2_notes", defaultValue = "")
    val phone2Notes: String?,
    @ColumnInfo("phone_search", defaultValue = "")
    val phoneSearch: String?,
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
    @ColumnInfo("network_photo_count", defaultValue = "0")
    val photoCount: Int?,

    // TODO Write tests throughout (model, data, edit feature)
    /**
     * Is relevant when [WorksiteRootEntity.isLocalModified] otherwise ignore
     */
    @ColumnInfo("is_local_favorite", defaultValue = "0")
    val isLocalFavorite: Boolean = false,
)

private val endNumbersCapture = Regex("(?:^|\\D)(\\d+)(?:\\D|$)")
fun parseCaseNumberOrder(caseNumber: String): Long {
    try {
        endNumbersCapture.find(caseNumber)?.let {
            if (it.groupValues.size > 1) {
                return it.groupValues[1].toLong()
            }
        }
    } catch (e: Exception) {
        Log.w("worksite-entity", "Unable to parse case number order ${e.message}")
    }
    return 0
}

@Entity(
    "work_types",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["worksite_id", "work_type"],
            unique = true,
            name = "unique_worksite_work_type",
        ),
        Index(value = ["worksite_id", "network_id"]),
        Index(value = ["status", "worksite_id"]),
        Index(value = ["claimed_by", "worksite_id"]),
        Index(value = ["network_id"]),
    ],
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
    val status: String,
    @ColumnInfo("work_type")
    val workType: String,
)

fun WorkTypeEntity.asExternalModel() = WorkType(
    id = id,
    createdAt = createdAt,
    orgClaim = orgClaim,
    nextRecurAt = nextRecurAt,
    phase = phase,
    recur = recur,
    statusLiteral = status,
    workTypeLiteral = workType,
)

@Entity(
    "worksite_form_data",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = ["worksite_id", "field_key"],
)
data class WorksiteFormDataEntity(
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    @ColumnInfo("field_key")
    val fieldKey: String,
    @ColumnInfo("is_bool_value")
    val isBoolValue: Boolean,
    @ColumnInfo("value_string")
    val valueString: String,
    @ColumnInfo("value_bool")
    val valueBool: Boolean,
)

@Entity(
    "worksite_flags",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["worksite_id", "reason_t"],
            unique = true,
            name = "unique_worksite_flag",
        ),
        Index(value = ["reason_t"]),
    ],
)
data class WorksiteFlagEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    val action: String?,
    @ColumnInfo("created_at")
    val createdAt: Instant,
    @ColumnInfo("is_high_priority", defaultValue = "0")
    val isHighPriority: Boolean?,
    @ColumnInfo(defaultValue = "")
    val notes: String?,
    @ColumnInfo("reason_t")
    val reasonT: String,
    @ColumnInfo("requested_action", defaultValue = "")
    val requestedAction: String?,
)

fun WorksiteFlagEntity.asExternalModel(translator: KeyTranslator? = null) = WorksiteFlag(
    id = id,
    action = action ?: "",
    createdAt = createdAt,
    isHighPriority = isHighPriority ?: false,
    notes = notes ?: "",
    reasonT = reasonT,
    reason = translator?.translate(reasonT) ?: reasonT,
    requestedAction = requestedAction ?: "",
    attr = null,
)

@Entity(
    "worksite_notes",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["worksite_id", "network_id", "local_global_uuid"],
            unique = true,
            name = "unique_worksite_note",
        ),
    ],
)
data class WorksiteNoteEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("local_global_uuid", defaultValue = "")
    val localGlobalUuid: String,
    // Notes cannot be invalidated (as of Mar 2023)

    @ColumnInfo("network_id", defaultValue = "-1")
    val networkId: Long,
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    @ColumnInfo("created_at")
    val createdAt: Instant,
    @ColumnInfo("is_survivor")
    val isSurvivor: Boolean,
    @ColumnInfo(defaultValue = "")
    val note: String,
)

fun WorksiteNoteEntity.asExternalModel() = WorksiteNote(
    id = id,
    createdAt = createdAt,
    isSurvivor = isSurvivor,
    note = note,
)
