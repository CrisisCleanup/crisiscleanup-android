package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.IterableStringSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class NetworkIncidentsResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkIncident>? = null,
)

@Serializable
data class NetworkIncidentResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val incident: NetworkIncident? = null,
)

@Serializable
data class NetworkIncidentLocation(
    val id: Long,
    val location: Long,
)

@Serializable
data class NetworkIncident(

    // UPDATE NetworkIncidentTest in conjunction with changes here

    val id: Long,
    @SerialName("start_at")
    val startAt: Instant,
    val name: String,
    @SerialName("short_name")
    val shortName: String,
    val locations: List<NetworkIncidentLocation>,
    @SerialName("incident_type")
    val type: String,
    @SerialName("turn_on_release")
    val turnOnRelease: Boolean?,
    @Serializable(IterableStringSerializer::class)
    @SerialName("active_phone_number")
    val activePhoneNumber: List<String>?,
    @SerialName("is_archived")
    val isArchived: Boolean?,

    @SerialName("form_fields")
    val fields: List<NetworkIncidentFormField>? = null,
)

@Serializable
data class NetworkIncidentFormField(
    @SerialName("label_t")
    val label: String,
    @SerialName("html_type")
    val htmlType: String,
    @SerialName("data_sensitivity")
    val dataSensitivity: String,
    @SerialName("data_group")
    val dataGroup: String,
    @SerialName("help_t")
    val help: String? = null,
    @SerialName("placeholder_t")
    val placeholder: String? = null,
    @SerialName("is_required_default")
    val isRequiredDefault: Boolean,
    @SerialName("is_read_only_default")
    val isReadOnlyDefault: Boolean,
    @SerialName("read_only_break_glass")
    val readOnlyBreakGlass: Boolean,
    @SerialName("values_default_t")
    val valuesDefault: Map<String, String>? = null,
    @SerialName("order_label")
    val orderLabel: Int? = null,
    val validation: String? = null,
    @SerialName("recur_default")
    val recurDefault: Boolean? = null,
    @SerialName("values")
    val values: List<FormFieldValue>? = null,
    @SerialName("is_required")
    val isRequired: Boolean? = null,
    @SerialName("is_read_only")
    val isReadOnly: Boolean? = null,
    @SerialName("list_order")
    val listOrder: Int,
    @SerialName("invalidated_at")
    val invalidatedAt: Instant? = null,
    @SerialName("field_key")
    val fieldKey: String,
    @SerialName("field_parent_key")
    val fieldParentKey: String? = null,
    @SerialName("if_selected_then_work_type")
    val selectToggleWorkType: String? = null,
    val phase: Int,
) {
    val checkboxDefault: Boolean
        get() = htmlType == "checkbox" && valuesDefault?.size == 1 && valuesDefault["value"] == "true"

    @Transient
    var isExpectedValueDefault: Boolean = false
        private set

    init {
        var isExpectedDefaults = false
        val valueCount = values?.size ?: 0
        if (valueCount > 0 && valuesDefault?.isNotEmpty() == true) {
            isExpectedDefaults = true
            values!!.map(FormFieldValue::value).onEach {
                if (it != null && valuesDefault[it] != it) {
                    isExpectedDefaults = false
                    return@onEach
                }
            }
        }
        isExpectedValueDefault = isExpectedDefaults
    }

    @Serializable
    data class FormFieldValue(
        val value: String? = null,
        @SerialName("name_t")
        val name: String,
    )
}
