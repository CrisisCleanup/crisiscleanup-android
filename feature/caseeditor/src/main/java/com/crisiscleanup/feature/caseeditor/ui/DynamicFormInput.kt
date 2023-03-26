package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue

@Composable
internal fun DynamicFormListItem(
    itemKey: String,
    label: String,
    fieldType: String,
    value: FieldDynamicValue,
    modifier: Modifier = Modifier,
    updateValue: (FieldDynamicValue) -> Unit = {},
) {
    when (fieldType) {
        "checkbox" -> {
            val updateBoolean = { b: Boolean ->
                val changedValue = value.copy(
                    dynamicValue = DynamicValue("", true, b)
                )
                updateValue(changedValue)
            }
            CheckboxListItem(
                modifier,
                value.dynamicValue.valueBoolean,
                text = label,
                onToggle = { updateBoolean(!value.dynamicValue.valueBoolean) },
                onCheckChange = { updateBoolean(it) }
            )
        }
        else -> {
            Text("$label $itemKey $fieldType")
        }
    }
}

@Composable
private fun CheckboxListItem(
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    @StringRes textResId: Int = 0,
    text: String = "",
    onToggle: () -> Unit = {},
    onCheckChange: (Boolean) -> Unit = {},
) {
    CrisisCleanupTextCheckbox(
        modifier,
        checked,
        textResId,
        text,
        onToggle,
        onCheckChange,
    )
}