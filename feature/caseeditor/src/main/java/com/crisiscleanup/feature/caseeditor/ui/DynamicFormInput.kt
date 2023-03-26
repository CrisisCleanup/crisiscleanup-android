package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue

@Composable
internal fun DynamicFormListItem(
    itemKey: String,
    label: String,
    fieldType: String,
    value: FieldDynamicValue,
    modifier: Modifier = Modifier,
    breakGlassHint: String = "",
    // TODO Incorporate help and showing
    helpText: String = "",
    showHelp: () -> Unit = {},
    updateValue: (FieldDynamicValue) -> Unit = {},
) {
    val updateString = if (value.dynamicValue.isBoolean) {
        {}
    } else { s: String ->
        val valueState = value.copy(dynamicValue = DynamicValue(s))
        updateValue(valueState)
    }
    val breakGlass = {
        val valueState = value.copy(isGlassBroken = true)
        updateValue(valueState)
    }
    when (fieldType) {
        "checkbox" -> {
            val updateBoolean = { b: Boolean ->
                val valueState = value.copy(
                    dynamicValue = DynamicValue("", true, b)
                )
                updateValue(valueState)
            }
            CheckboxListItem(
                modifier,
                value.dynamicValue.valueBoolean,
                text = label,
                onToggle = { updateBoolean(!value.dynamicValue.valueBoolean) },
                onCheckChange = { updateBoolean(it) }
            )
        }
        "text" -> {
            SingleLineTextItem(
                modifier,
                value.dynamicValue.valueString,
                label = label,
                onChange = { updateString(it) },
                isGlass = value.isGlass,
                isGlassBroken = value.isGlassBroken,
                breakGlassHint = breakGlassHint,
                onBreakGlass = breakGlass,
                breakGlassFocus = value.takeBrokenGlassFocus(),
            )
        }
        "textarea" -> {
            MultiLineTextItem(
                modifier,
                value.dynamicValue.valueString,
                label = label,
                onChange = { updateString(it) },
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

@Composable
private fun SingleLineTextItem(
    modifier: Modifier = Modifier,
    value: String = "",
    @StringRes labelResId: Int = 0,
    label: String = "",
    onChange: (String) -> Unit = {},
    isGlass: Boolean = false,
    isGlassBroken: Boolean = false,
    breakGlassHint: String = "",
    onBreakGlass: () -> Unit = {},
    breakGlassFocus: Boolean = false,
) {
    val isGlassState = isGlass && !isGlassBroken
    val rowModifier = if (isGlassState) modifier else Modifier
    Row(
        rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textFieldModifier = if (isGlassState) Modifier.weight(1f)
        else modifier

        // TODO Figure out why OutlinedClearableTextField won't focus with flag and needs focus code here.
        val focusRequester = FocusRequester()
        val focusModifier =
            if (breakGlassFocus) textFieldModifier.then(Modifier.focusRequester(focusRequester)) else textFieldModifier

        OutlinedClearableTextField(
            modifier = focusModifier,
            labelResId = labelResId,
            label = label,
            value = value,
            onValueChange = onChange,
            enabled = !isGlass || isGlassBroken,
            isError = false,
            keyboardType = KeyboardType.Password,
            // TODO Support done with closeKeyboard behavior if last item list
            imeAction = ImeAction.Next,
            hasFocus = breakGlassFocus,
        )
        if (isGlassState) {
            IconButton(onClick = onBreakGlass) {
                Icon(
                    imageVector = CrisisCleanupIcons.Edit,
                    contentDescription = breakGlassHint,
                )
            }
        } else if (breakGlassFocus) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiLineTextItem(
    modifier: Modifier = Modifier,
    value: String = "",
    @StringRes labelResId: Int = 0,
    label: String = "",
    onChange: (String) -> Unit = {},
    helpText: String = "",
) {
    if (helpText.isNotBlank()) {
        Row(modifier) {
            // TODO Help text opens
        }
    }

    // TODO Next or done (if last in list). Callback to next/done action as well
    val keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Password,
    )
    val focusManager = LocalFocusManager.current
    val keyboardActions = KeyboardActions(
        onNext = {
            focusManager.moveFocus(FocusDirection.Down)
        },
        onDone = {
        },
    )
    OutlinedTextField(
        value,
        onValueChange = onChange,
        // TODO Use common dimensions
        modifier = modifier.heightIn(min = 128.dp, max = 256.dp),
        label = { Text(label) },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}