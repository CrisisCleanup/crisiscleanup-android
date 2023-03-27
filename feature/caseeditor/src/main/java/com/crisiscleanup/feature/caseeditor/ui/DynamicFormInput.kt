package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue

@Composable
internal fun DynamicFormListItem(
    field: FieldDynamicValue,
    label: String,
    modifier: Modifier = Modifier,
    breakGlassHint: String = "",
    helpHint: String = "",
    showHelp: () -> Unit = {},
    updateValue: (FieldDynamicValue) -> Unit = {},
) {
    val updateString = if (field.dynamicValue.isBoolean) {
        {}
    } else { s: String ->
        val valueState = field.copy(dynamicValue = DynamicValue(s))
        updateValue(valueState)
    }
    val breakGlass = {
        val breakGlass = field.breakGlass.copy(isGlassBroken = true)
        val valueState = field.copy(breakGlass = breakGlass)
        updateValue(valueState)
    }
    when (field.field.htmlType) {
        "checkbox" -> {
            val updateBoolean = { b: Boolean ->
                val valueState = field.copy(
                    dynamicValue = DynamicValue("", true, b)
                )
                updateValue(valueState)
            }
            CheckboxItem(
                field,
                modifier,
                text = label,
                onToggle = { updateBoolean(!field.dynamicValue.valueBoolean) },
                onCheckChange = { updateBoolean(it) },
                helpHint,
                showHelp,
            )
        }
        "text" -> {
            SingleLineTextItem(
                field,
                modifier,
                label = label,
                onChange = { updateString(it) },
                breakGlassHint = breakGlassHint,
                onBreakGlass = breakGlass,
                helpHint,
                showHelp,
            )
        }
        "textarea" -> {
            MultiLineTextItem(
                field,
                modifier,
                label = label,
                onChange = { updateString(it) },
                helpHint,
                showHelp,
            )
        }
        "select" -> {
            SelectItem(
                field,
                modifier,
                label = label,
                onChange = { updateString(it) },
                helpHint,
                showHelp,
            )
        }
        "h5",
        "h4" -> {
            Text(
                modifier = modifier,
                text = label,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        else -> {
            Text("$label ${field.key} ${field.field.htmlType}")
        }
    }
}

@Composable
private fun HelpAction(
    helpHint: String,
    showHelp: () -> Unit,
) {
    IconButton(onClick = showHelp) {
        Icon(
            imageVector = CrisisCleanupIcons.Help,
            contentDescription = helpHint,
        )
    }
}

@Composable
private fun CheckboxItem(
    itemData: FieldDynamicValue,
    modifier: Modifier = Modifier,
    text: String = "",
    onToggle: () -> Unit = {},
    onCheckChange: (Boolean) -> Unit = {},
    helpHint: String,
    showHelp: () -> Unit = {},
) {
    val helpAction: (@Composable () -> Unit)? = if (itemData.field.help.isBlank()) null
    else {
        @Composable {
            HelpAction(helpHint, showHelp)
        }
    }
    CrisisCleanupTextCheckbox(
        // TODO Common dimensions
        modifier.offset(x = (-12).dp),
        itemData.dynamicValue.valueBoolean,
        0,
        text,
        onToggle,
        onCheckChange,
        trailingContent = helpAction,
    )
}

@Composable
private fun SingleLineTextItem(
    itemData: FieldDynamicValue,
    modifier: Modifier = Modifier,
    label: String = "",
    onChange: (String) -> Unit = {},
    breakGlassHint: String = "",
    onBreakGlass: () -> Unit = {},
    helpHint: String,
    showHelp: () -> Unit = {},
) {
    val glassState = itemData.breakGlass
    val isGlass = glassState.isNotEditable
    val rowModifier = if (isGlass) modifier else Modifier
    Row(
        rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textFieldModifier = if (isGlass) Modifier.weight(1f)
        else modifier

        // TODO Figure out why OutlinedClearableTextField won't focus with flag and needs focus code here.
        val focusRequester = FocusRequester()
        val hasFocus = glassState.takeBrokenGlassFocus()
        val focusModifier =
            if (hasFocus) textFieldModifier.then(Modifier.focusRequester(focusRequester)) else textFieldModifier

        OutlinedClearableTextField(
            modifier = focusModifier,
            labelResId = 0,
            label = label,
            value = itemData.dynamicValue.valueString,
            onValueChange = onChange,
            enabled = !isGlass,
            isError = false,
            keyboardType = KeyboardType.Password,
            // TODO Support done with closeKeyboard behavior if last item list
            imeAction = ImeAction.Next,
            hasFocus = hasFocus,
        )

        if (isGlass) {
            IconButton(
                // TODO Use common dimensions
                modifier = Modifier.padding(start = 8.dp),
                onClick = onBreakGlass
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.Edit,
                    contentDescription = breakGlassHint,
                )
            }
        } else if (hasFocus) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        if (itemData.field.help.isNotBlank()) {
            HelpAction(helpHint, showHelp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiLineTextItem(
    itemData: FieldDynamicValue,
    modifier: Modifier = Modifier,
    label: String = "",
    onChange: (String) -> Unit = {},
    helpHint: String,
    showHelp: () -> Unit = {},
) {
    val hasHelp = itemData.field.help.isNotBlank()
    val helpModifier = Modifier
        .fillMaxWidth()
        // TODO Common dimensions
        .padding(horizontal = 16.dp)
    if (hasHelp) {
        Row(
            helpModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
            )
            HelpAction(helpHint, showHelp)
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
    // TODO Common dimensions
    val textFieldModifier = if (hasHelp) helpModifier.padding(bottom = 8.dp)
    else modifier
    OutlinedTextField(
        itemData.dynamicValue.valueString,
        onValueChange = onChange,
        // TODO Use common dimensions
        modifier = textFieldModifier.heightIn(min = 128.dp, max = 256.dp),
        label = { Text(label) },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Composable
private fun SelectItem(
    itemData: FieldDynamicValue,
    modifier: Modifier = Modifier,
    label: String = "",
    onChange: (String) -> Unit = {},
    helpHint: String,
    showHelp: () -> Unit = {},
) {
    Box(Modifier.fillMaxWidth()) {
        var contentWidth by remember { mutableStateOf(Size.Zero) }
        var showDropdown by remember { mutableStateOf(false) }
        BackHandler(showDropdown) {
            showDropdown = false
        }
        Column(
            Modifier
                .clickable { showDropdown = true }
                .fillMaxWidth()
                // TODO Common dimensions
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onGloballyPositioned {
                    contentWidth = it.size.toSize()
                },
        ) {
            Row(
                // TODO Common dimensions
                Modifier.heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                )
                if (itemData.field.help.isNotBlank()) {
                    HelpAction(helpHint, showHelp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = CrisisCleanupIcons.UnfoldMore,
                    // TODO Options for ...
                    contentDescription = "",
                )
            }
            val selectedOption = itemData.dynamicValue.valueString
            if (selectedOption.isNotBlank()) {
                Text(
                    selectedOption,
                    // TODO Common dimensions
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
        }

        if (showDropdown && itemData.selectOptions.isNotEmpty()) {
            val onSelect = { key: String ->
                onChange(key)
                showDropdown = false
            }
            DropdownMenu(
                modifier = Modifier
                    .width(with(LocalDensity.current) { contentWidth.width.toDp() }),
                expanded = true,
                onDismissRequest = { showDropdown = false },
                // TODO Common dimensions
                offset = DpOffset(16.dp, 0.dp),
                properties = PopupProperties(focusable = false)
            ) {
                DropdownItems(
                    itemData.selectOptions,
                    { onSelect(it) },
                    true,
                )
            }
        }
    }
}

@Composable
private fun DropdownItems(
    options: Map<String, String>,
    onSelect: (String) -> Unit,
    addEmptyOption: Boolean = false,
) {
    if (addEmptyOption) {
        DropdownMenuItem(
            text = { Text("") },
            onClick = { onSelect("") },
        )
    }
    for (option in options) {
        key(option.key) {
            DropdownMenuItem(
                text = { Text(option.value) },
                onClick = { onSelect(option.key) },
            )
        }
    }
}