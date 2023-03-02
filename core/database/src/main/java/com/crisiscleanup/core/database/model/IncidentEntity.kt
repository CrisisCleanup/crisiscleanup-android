package com.crisiscleanup.core.database.model

import androidx.room.*
import com.crisiscleanup.core.model.data.IncidentFormField
import com.crisiscleanup.core.model.data.IncidentLocation
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Entity(
    "incidents",
    [
        Index(
            value = ["start_at"],
            orders = [Index.Order.DESC],
            name = "idx_newest_to_oldest_incidents",
        )
    ]
)
data class IncidentEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("start_at")
    val startAt: Instant,
    val name: String,
    @ColumnInfo("short_name", defaultValue = "")
    val shortName: String,
    @ColumnInfo("incident_type", defaultValue = "")
    val type: String,
    // Comma delimited phone numbers if defined
    @ColumnInfo("active_phone_number", defaultValue = "")
    val activePhoneNumber: String? = null,
    @ColumnInfo("is_archived", defaultValue = "0")
    val isArchived: Boolean = false,
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
    ]
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
    primaryKeys = ["incident_id", "field_key"],
    indices = [
        Index(value = ["data_group", "field_parent_key", "list_order"]),
    ]
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
    @ColumnInfo("field_parent_key", defaultValue = "")
    val fieldParentKey: String?,
    @ColumnInfo("selected_toggle_work_type", defaultValue = "")
    val selectToggleWorkType: String?,
)

fun IncidentFormFieldEntity.asExternalModel(): IncidentFormField {
    val formValues =
        if (valuesJson?.isNotEmpty() == true) Json.decodeFromString<Map<String, String>>(valuesJson) else emptyMap()
    val formValuesDefault =
        if (formValues.isEmpty() && valuesDefaultJson?.isNotEmpty() == true) Json.decodeFromString<Map<String, String>>(
            valuesDefaultJson
        ) else emptyMap()
    return IncidentFormField(
        label = label,
        htmlType = htmlType,
        group = dataGroup,
        help = help ?: "",
        placeholder = placeholder ?: "",
        validation = validation ?: "",
        readOnlyBreakGlass = readOnlyBreakGlass,
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
        parentKey = fieldParentKey ?: "",
        selectToggleWorkType = selectToggleWorkType ?: "",
    )
}