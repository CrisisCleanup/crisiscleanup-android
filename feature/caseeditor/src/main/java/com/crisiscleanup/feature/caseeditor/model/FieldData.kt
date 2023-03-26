package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.IncidentFormField
import com.crisiscleanup.core.network.model.DynamicValue

class FieldState(
    val node: FormFieldNode,
    val field: IncidentFormField = node.formField,
) {
    val listItemContentType = "item-${field.htmlType}"
}

data class FieldDynamicValue(
    val dynamicValue: DynamicValue = DynamicValue(""),
    val isGlass: Boolean = false,
    val isGlassBroken: Boolean = false,
) {
    private var brokenGlassFocus = true

    fun takeBrokenGlassFocus(): Boolean {
        if (brokenGlassFocus) {
            brokenGlassFocus = false
            return true
        }
        return false
    }
}