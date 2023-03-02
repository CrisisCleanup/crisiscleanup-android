package com.crisiscleanup.core.model.data

data class Incident(
    val id: Long,
    val name: String,
    val shortName: String,
    val locations: List<IncidentLocation>,
    val activePhoneNumbers: List<String>,
    val formFields: List<IncidentFormField>,
)

val EmptyIncident = Incident(-1, "", "", emptyList(), emptyList(), emptyList())

data class IncidentLocation(
    val id: Long,
    val location: Long,
)

data class IncidentFormField(
    val label: String,
    val htmlType: String,
    val group: String,
    val help: String,
    val placeholder: String,
    val validation: String,
    val readOnlyBreakGlass: Boolean,
    val valuesDefault: Map<String, String>?,
    val values: Map<String, String>,
    val isCheckboxDefaultTrue: Boolean,
    val recurDefault: String,
    val isRequired: Boolean,
    val isReadOnly: Boolean,
    val labelOrder: Int,
    val listOrder: Int,
    val isInvalidated: Boolean,
    val fieldKey: String,
    val parentKey: String,
    val selectToggleWorkType: String,
)