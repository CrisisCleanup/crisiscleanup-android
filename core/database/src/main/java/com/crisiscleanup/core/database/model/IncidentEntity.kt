package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.IncidentFormField
import com.crisiscleanup.core.model.data.IncidentLocation
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

@Entity(
    "incidents",
    [
        Index(
            value = ["start_at"],
            orders = [Index.Order.DESC],
            name = "idx_newest_to_oldest_incidents",
        ),
    ],
)
data class IncidentEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("start_at")
    val startAt: Instant,
    val name: String,
    @ColumnInfo("short_name", defaultValue = "")
    val shortName: String,
    @ColumnInfo("case_label", defaultValue = "")
    val caseLabel: String,
    @ColumnInfo("incident_type", defaultValue = "")
    val type: String,
    // Comma delimited phone numbers if defined
    @ColumnInfo("active_phone_number", defaultValue = "")
    val activePhoneNumber: String? = null,
    @ColumnInfo("turn_on_release", defaultValue = "0")
    val turnOnRelease: Boolean = false,
    @ColumnInfo("is_archived", defaultValue = "0")
    val isArchived: Boolean = false,
    @ColumnInfo("ignore_claiming_thresholds", defaultValue = "0")
    val ignoreClaimingThresholds: Boolean = false,
)

@Entity("incident_locations")
data class IncidentLocationEntity(
    // location.id
    @PrimaryKey
    val id: Long,
    // location.location
    val location: Long,
)

fun IncidentLocationEntity.asExternalModel() = IncidentLocation(
    id = id,
    location = location,
)

@Entity(
    "incident_to_incident_location",
    primaryKeys = ["incident_id", "incident_location_id"],
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IncidentLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_location_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["incident_location_id", "incident_id"],
            name = "idx_incident_location_to_incident",
        ),
    ],
)
data class IncidentIncidentLocationCrossRef(
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("incident_location_id")
    val incidentLocationId: Long,
)

@Entity(
    "incident_form_fields",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // TODO Write test inserting fields with different parents same key
    primaryKeys = ["incident_id", "parent_key", "field_key"],
    indices = [
        Index(value = ["data_group", "parent_key", "list_order"]),
    ],
)
data class IncidentFormFieldEntity(
    @ColumnInfo("incident_id")
    val incidentId: Long,
    val label: String,
    @ColumnInfo("html_type")
    val htmlType: String,
    @ColumnInfo("data_group")
    val dataGroup: String,
    @ColumnInfo(defaultValue = "")
    val help: String?,
    @ColumnInfo(defaultValue = "")
    val placeholder: String?,
    @ColumnInfo("read_only_break_glass")
    val readOnlyBreakGlass: Boolean,
    @ColumnInfo("values_default_json", defaultValue = "")
    val valuesDefaultJson: String?,
    @ColumnInfo("is_checkbox_default_true", defaultValue = "0")
    val isCheckboxDefaultTrue: Boolean?,
    @ColumnInfo("order_label", defaultValue = "-1")
    val orderLabel: Int,
    @ColumnInfo(defaultValue = "")
    val validation: String?,
    @ColumnInfo("recur_default", defaultValue = "0")
    val recurDefault: String?,
    @ColumnInfo("values_json", defaultValue = "")
    val valuesJson: String?,
    @ColumnInfo("is_required", defaultValue = "0")
    val isRequired: Boolean?,
    @ColumnInfo("is_read_only", defaultValue = "0")
    val isReadOnly: Boolean?,
    @ColumnInfo("list_order")
    val listOrder: Int,
    @ColumnInfo("is_invalidated")
    val isInvalidated: Boolean,
    @ColumnInfo("field_key")
    val fieldKey: String,
    @Deprecated(message = "Use parentKeyNonNull", replaceWith = ReplaceWith("parentKeyNonNull"))
    @ColumnInfo("field_parent_key", defaultValue = "")
    val fieldParentKey: String? = null,
    @ColumnInfo("parent_key", defaultValue = "")
    val parentKeyNonNull: String,
    @ColumnInfo("selected_toggle_work_type", defaultValue = "")
    val selectToggleWorkType: String?,
)

fun IncidentFormFieldEntity.asExternalModel(): IncidentFormField {
    val formValues =
        if (valuesJson?.isNotEmpty() == true) Json.decodeFromString<Map<String, String>>(valuesJson) else emptyMap()
    val formValuesDefault =
        if (formValues.isEmpty() && valuesDefaultJson?.isNotEmpty() == true) {
            Json.decodeFromString<Map<String, String?>>(
                valuesDefaultJson,
            )
        } else {
            emptyMap()
        }
    return IncidentFormField(
        label = label,
        htmlType = htmlType,
        group = dataGroup,
        help = help ?: "",
        placeholder = placeholder ?: "",
        validation = validation ?: "",
        isReadOnlyBreakGlass = readOnlyBreakGlass,
        valuesDefault = formValuesDefault,
        values = formValues,
        isCheckboxDefaultTrue = isCheckboxDefaultTrue ?: false,
        recurDefault = recurDefault ?: "",
        isRequired = isRequired ?: false,
        isReadOnly = isReadOnly ?: false,
        labelOrder = orderLabel,
        listOrder = listOrder,
        isInvalidated = isInvalidated,
        fieldKey = fieldKey,
        parentKey = parentKeyNonNull,
        selectToggleWorkType = selectToggleWorkType ?: "",
    )
}
