package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.feature.caseeditor.R
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
    enabled: Boolean = true,
    translate: (String) -> String = { s -> s },
    workTypeStatusOptions: List<WorkTypeStatus> = emptyList(),
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
                label,
                helpHint,
                showHelp,
                enabled,
            )
        }
        "text" -> {
            SingleLineTextItem(
                field,
                modifier,
                label,
                { updateString(it) },
                breakGlassHint,
                breakGlass,
                helpHint,
                showHelp,
                enabled,
            )
        }
        "textarea" -> {
            MultiLineTextItem(
                field,
                modifier,
                label,
                { updateString(it) },
                helpHint,
                showHelp,
                enabled,
            )
        }
        "select" -> {
            SelectItem(
                field,
                modifier,
                label,
                { updateString(it) },
                helpHint,
                showHelp,
                enabled,
            )
        }
        "h5",
        "h4" -> {
            if (field.childrenCount == 0 && field.field.isReadOnly) {
                Text(
                    modifier = modifier,
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                val updateGroupExpandValue = { value: FieldDynamicValue ->
                    groupExpandState[field.key] = value.dynamicValue.isBooleanTrue
                    updateValue(value)
                }
                val isActiveWorkType = field.dynamicValue.isBooleanTrue && field.isWorkTypeGroup
                val updateWorkTypeStatus = { status: WorkTypeStatus ->
                    if (isActiveWorkType) {
                        val valueState = field.copy(workTypeStatus = status)
                        updateValue(valueState)
                    }
                }
                CheckboxItem(
                    field,
                    updateGroupExpandValue,
                    modifier,
                    label,
                    helpHint,
                    showHelp,
                    enabled,
                    translate = translate,
                    statusOptions = workTypeStatusOptions,
                    updateWorkTypeStatus = updateWorkTypeStatus,
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
    updateValue: (FieldDynamicValue) -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    helpHint: String,
    showHelp: () -> Unit = {},
    enabled: Boolean = true,
    translate: (String) -> String = { s -> s },
    statusOptions: List<WorkTypeStatus> = emptyList(),
    updateWorkTypeStatus: (WorkTypeStatus) -> Unit = {},
) {
    val updateBoolean = { b: Boolean ->
        val valueState = itemData.copy(
            dynamicValue = DynamicValue("", true, b)
        )
        updateValue(valueState)
    }

    val isChecked = itemData.dynamicValue.valueBoolean
    val isActiveWorkType = isChecked && itemData.isWorkTypeGroup

    val trailingContent: (@Composable () -> Unit)? = if (isActiveWorkType) {
        @Composable {
            WorkTypeStatusDropdown(itemData, updateWorkTypeStatus, translate, statusOptions)
        }
    } else if (itemData.field.help.isNotBlank()) {
        @Composable {
            HelpAction(helpHint, showHelp)
        }
    } else null

    CrisisCleanupTextCheckbox(
        modifier.listCheckboxAlignStartOffset(),
        isChecked,
        0,
        text,
        { updateBoolean(!itemData.dynamicValue.valueBoolean) },
        { updateBoolean(it) },
        trailingContent = trailingContent,
        enabled = enabled,
        enableToggle = !isActiveWorkType,
        spaceTrailingContent = itemData.isWorkTypeGroup,
    )
}

@Composable
private fun WorkTypeStatusDropdown(
    itemData: FieldDynamicValue,
    updateWorkTypeStatus: (WorkTypeStatus) -> Unit,
    translate: (String) -> String = { s -> s },
    statusOptions: List<WorkTypeStatus> = emptyList(),
) {
    // TODO Colors (indicators) and shape
    val status = translate(itemData.workTypeStatus.literal)
    var showOptions by remember { mutableStateOf(false) }
    Box {
        Text(
            status,
            modifier = Modifier
                .clickable(
                    enabled = statusOptions.isNotEmpty(),
                    onClick = { showOptions = true },
                )
                .listItemPadding()
                .clip(RoundedCornerShape(8.dp)),
            style = MaterialTheme.typography.bodySmall
        )

        if (showOptions && statusOptions.isNotEmpty()) {
            val onSelect = { selected: WorkTypeStatus ->
                updateWorkTypeStatus(selected)
                showOptions = false
            }
            DropdownMenu(
                expanded = true,
                onDismissRequest = { showOptions = false },
                offset = listItemDropdownMenuOffset,
                properties = PopupProperties(focusable = false),
            ) {
                WorkTypeStatusOptions(
                    onSelect,
                    statusOptions,
                    translate,
                )
            }
        }
    }
}

@Composable
private fun WorkTypeStatusOptions(
    onSelect: (WorkTypeStatus) -> Unit = {},
    statusOptions: List<WorkTypeStatus> = emptyList(),
    translate: (String) -> String = { s -> s },
) {
    for (option in statusOptions) {
        DropdownMenuItem(
            modifier = Modifier.optionItemHeight(),
            text = {
                Text(
                    translate(option.literal),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            onClick = { onSelect(option) },
        )
    }
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
    enabled: Boolean = true,
) {
    val glassState = itemData.breakGlass
    val isGlass = glassState.isNotEditable
    val hasHelp = itemData.field.help.isNotBlank()
    val hasMultipleRowItems = isGlass || hasHelp
    val rowModifier = if (hasMultipleRowItems) modifier else Modifier
    Row(
        rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textFieldModifier = if (hasMultipleRowItems) Modifier.weight(1f)
        else modifier

        val hasFocus = if (glassState.isGlassBroken) glassState.takeBrokenGlassFocus() else false

        OutlinedClearableTextField(
            modifier = textFieldModifier,
            labelResId = 0,
            label = label,
            value = itemData.dynamicValue.valueString,
            onValueChange = onChange,
            enabled = !isGlass && enabled,
            isError = false,
            keyboardType = KeyboardType.Password,
            // TODO Support done with closeKeyboard behavior if last item list
            imeAction = ImeAction.Next,
            hasFocus = hasFocus,
        )

        if (isGlass) {
            IconButton(
                modifier = Modifier
                    .listRowItemStartPadding()
                    .centerAlignTextFieldLabelOffset(),
                onClick = onBreakGlass,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.Edit,
                    contentDescription = breakGlassHint,
                )
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
    enabled: Boolean = true,
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
            enabled = enabled,
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
    enabled: Boolean = true,
) {
    Box(Modifier.fillMaxWidth()) {
        var contentWidth by remember { mutableStateOf(Size.Zero) }
        var showDropdown by remember { mutableStateOf(false) }
        BackHandler(showDropdown) {
            showDropdown = false
        }
        Column(
            Modifier
                .clickable(
                    onClick = { showDropdown = true },
                    enabled = enabled,
                )
                .fillMaxWidth()
                .onGloballyPositioned {
                    contentWidth = it.size.toSize()
                }
                .then(modifier),
        ) {
            val selectedOption = itemData.dynamicValue.valueString
            val hasSelection = selectedOption.isNotBlank()
            Row(
                Modifier.listItemVerticalPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label)
                if (itemData.field.help.isNotBlank()) {
                    HelpAction(helpHint, showHelp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = CrisisCleanupIcons.UnfoldMore,
                    contentDescription = stringResource(R.string.select_option_for_field, label),
                )
            }
            if (hasSelection) {
                Text(
                    itemData.selectOptions[selectedOption] ?: selectedOption,
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
