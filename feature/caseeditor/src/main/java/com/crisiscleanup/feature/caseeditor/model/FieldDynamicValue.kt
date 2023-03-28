package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.IncidentFormField
import com.crisiscleanup.core.network.model.DynamicValue

data class FieldDynamicValue(
    val field: IncidentFormField,
    val selectOptions: Map<String, String>,
    val childKeys: Set<String> = emptySet(),
    val nestLevel: Int = 0,
    val dynamicValue: DynamicValue = DynamicValue(""),
    val breakGlass: FieldEditProperties = FieldEditProperties(field.isReadOnlyBreakGlass),
) {
    val key = field.fieldKey
    val childrenCount = childKeys.size
}

data class FieldEditProperties(
    val isGlass: Boolean,
    val isGlassBroken: Boolean = false,
) {
    val isNotEditable = isGlass && !isGlassBroken

    private var brokenGlassFocus = true

    fun takeBrokenGlassFocus(): Boolean {
        if (brokenGlassFocus) {
            brokenGlassFocus = false
            return true
        }
        return false
    }
}