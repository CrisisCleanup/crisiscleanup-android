package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.IterableStringSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("case_label")
    val caseLabel: String,
    val locations: List<NetworkIncidentLocation>,
    @SerialName("incident_type")
    val type: String,
    @Serializable(IterableStringSerializer::class)
    @SerialName("active_phone_number")
    val activePhoneNumber: List<String>?,
    @SerialName("turn_on_release")
    val turnOnRelease: Boolean,
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
    @SerialName("data_group")
    val dataGroup: String,
    @SerialName("help_t")
    val help: String? = null,
    @SerialName("placeholder_t")
    val placeholder: String? = null,
    @SerialName("read_only_break_glass")
    val readOnlyBreakGlass: Boolean,
    @SerialName("values_default_t")
    val valuesDefault: Map<String, String?>? = null,
    @SerialName("order_label")
    val orderLabel: Int? = null,
    val validation: String? = null,
    @SerialName("recur_default")
    val recurDefault: String? = null,
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
) {
    val isCheckboxDefaultTrue: Boolean
        get() = htmlType == "checkbox" && valuesDefault?.size == 1 && valuesDefault["value"] == "true"

    @Serializable
    data class FormFieldValue(
        val value: String? = null,
        @SerialName("name_t")
        val name: String,
    )
}

@Serializable
data class NetworkIncidentsListResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkIncidentShort>? = null,
)

@Serializable
data class NetworkIncidentShort(
    val id: Long,
    val name: String,
    @SerialName("short_name")
    val shortName: String,
    @SerialName("incident_type")
    val type: String,
)
