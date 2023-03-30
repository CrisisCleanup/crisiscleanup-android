package com.crisiscleanup.core.model.data

data class Incident(
    val id: Long,
    val name: String,
    val shortName: String,
    val locations: List<IncidentLocation>,
    val activePhoneNumbers: List<String>,
    val formFields: List<IncidentFormField>,
) {
    val formFieldLookup: Map<String, IncidentFormField> by lazy {
        formFields.associateBy { it.fieldKey }
    }

    /**
     * Form data fields categorized under a work type
     */
    val workTypeLookup: Map<String, String> by lazy {
        formFields
            .filter { it.selectToggleWorkType.isNotBlank() }
            .associate { it.fieldKey to it.selectToggleWorkType }
    }
}

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
    val valuesDefault: Map<String, String?>?,
    val values: Map<String, String>,
    val isCheckboxDefaultTrue: Boolean,
    val recurDefault: String,
    val isRequired: Boolean,
    val isReadOnly: Boolean,
    val isReadOnlyBreakGlass: Boolean,
    val labelOrder: Int,
    val listOrder: Int,
    val isInvalidated: Boolean,
    val fieldKey: String,
    val parentKey: String,
    val selectToggleWorkType: String,
) {
    val isDivEnd = htmlType.lowercase() == "divend"
    val isHidden = htmlType.lowercase() == "hidden"

    fun getFieldLabel(translate: (String) -> String) = label.ifBlank { translate(fieldKey) }
}