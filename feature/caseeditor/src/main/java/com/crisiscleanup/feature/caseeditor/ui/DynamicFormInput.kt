package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue

@Composable
internal fun DynamicFormListItem(
    field: FieldDynamicValue,
    label: String,
    groupExpandState: SnapshotStateMap<String, Boolean>,
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
            CheckboxItem(
                field,
                updateValue,
                modifier,
                text = label,
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
        // TODO h5 should expand/collapse child views if children exist
        "h5",
        "h4" -> {
            val updateGroupValue = { value: FieldDynamicValue ->
                groupExpandState[field.key] = value.dynamicValue.isBooleanTrue
                updateValue(value)
            }
            if (field.childrenCount > 0) {
                CheckboxItem(
                    field,
                    updateGroupValue,
                    modifier,
                    text = label,
                    helpHint,
                    showHelp,
                )
            } else {
                Text(
                    modifier = modifier,
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        else -> {
            Text("$label ${field.key} ${field.field.htmlType}")
        }
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
        modifier.listCheckboxAlignStartOffset(),
        itemData.dynamicValue.valueBoolean,
        0,
        text,
        onToggle,
        onCheckChange,
        trailingContent = helpAction,
    )
}

@Composable
private fun CheckboxItem(
    itemData: FieldDynamicValue,
    updateValue: (FieldDynamicValue) -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    helpHint: String,
    showHelp: () -> Unit = {},
) {
    val updateBoolean = { b: Boolean ->
        val valueState = itemData.copy(
            dynamicValue = DynamicValue("", true, b)
        )
        updateValue(valueState)
    }
    CheckboxItem(
        itemData,
        modifier,
        text = text,
        onToggle = { updateBoolean(!itemData.dynamicValue.valueBoolean) },
        onCheckChange = { updateBoolean(it) },
        helpHint,
        showHelp,
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
    val hasHelp = itemData.field.help.isNotBlank()
    val hasMultipleRowItems = isGlass || hasHelp
    val rowModifier = if (hasMultipleRowItems) modifier else Modifier
    Row(
        rowModifier,
        // TODO Alignment is off by space due to hint
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textFieldModifier = if (hasMultipleRowItems) Modifier.weight(1f)
        else modifier

        // TODO Figure out why OutlinedClearableTextField won't focus with flag and needs focus code here.
        val focusRequester = FocusRequester()
        val hasFocus = if (glassState.isGlassBroken) glassState.takeBrokenGlassFocus() else false
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
                modifier = Modifier.listRowItemStartPadding(),
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

        if (hasHelp) {
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
    Column(modifier) {
        val hasHelp = itemData.field.help.isNotBlank()
        if (hasHelp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
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
        OutlinedTextField(
            itemData.dynamicValue.valueString,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .textBoxHeight(),
            label = { Text(label) },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
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
                .onGloballyPositioned {
                    contentWidth = it.size.toSize()
                }
                .then(modifier),
        ) {
            Row(
                Modifier.listItemHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label)
                if (itemData.field.help.isNotBlank()) {
                    HelpAction(helpHint, showHelp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = CrisisCleanupIcons.UnfoldMore,
                    // TODO String res "Select option for %s"
                    contentDescription = "",
                )
            }
            val selectedOption = itemData.dynamicValue.valueString
            if (selectedOption.isNotBlank()) {
                Text(
                    selectedOption,
                    modifier = Modifier
                        .listItemHorizontalPadding()
                        .listItemBottomPadding()
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
                    .width(with(LocalDensity.current) {
                        contentWidth.width.toDp().minus(listItemDropdownMenuOffset.x.times(2))
                    }),
                expanded = true,
                onDismissRequest = { showDropdown = false },
                offset = listItemDropdownMenuOffset,
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
            modifier = Modifier.optionItemHeight(),
            text = { Text("") },
            onClick = { onSelect("") },
        )
    }
    for (option in options) {
        key(option.key) {
            DropdownMenuItem(
                modifier = Modifier.optionItemHeight(),
                text = { Text(option.value) },
                onClick = { onSelect(option.key) },
            )
        }
    }
}