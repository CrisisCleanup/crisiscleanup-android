package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.toSize
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupFilterChip
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.HelpAction
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.centerAlignTextFieldLabelOffset
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue

private const val FallbackRrule = "RRULE:FREQ=WEEKLY;BYDAY=MO;INTERVAL=1;BYHOUR=11"

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
    updateValue: (FieldDynamicValue) -> Unit = {},
) {
    val updateBoolean = remember(field) {
        { b: Boolean ->
            val dynamicValue = DynamicValue("", true, b)
            val valueState = field.copy(dynamicValue = dynamicValue)
            updateValue(valueState)
        }
    }
    val updateString = remember(field) {
        { s: String ->
            val valueState = field.copy(dynamicValue = DynamicValue(s))
            updateValue(valueState)
        }
    }
    val breakGlass = remember(field) {
        {
            if (field.breakGlass.isGlass) {
                val breakGlass = field.breakGlass.copy(isGlassBroken = true)
                val valueState = field.copy(breakGlass = breakGlass)
                updateValue(valueState)
            }
        }
    }
    when (field.field.htmlType) {
        "checkbox" -> {
            CheckboxItem(
                field,
                updateBoolean,
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
                updateString,
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
                updateString,
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
                updateString,
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
                val updateGroupExpandValue = remember(field) {
                    { b: Boolean ->
                        groupExpandState[field.key] = b
                        updateBoolean(b)
                    }
                }
                val updateWorkTypeStatus = remember(field) {
                    { status: WorkTypeStatus ->
                        val isActiveWorkType =
                            field.dynamicValue.isBooleanTrue && field.isWorkTypeGroup
                        if (isActiveWorkType) {
                            val valueState = field.copy(workTypeStatus = status)
                            updateValue(valueState)
                        }
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
                    updateWorkTypeStatus = updateWorkTypeStatus,
                )
            }
        }

        "multiselect" -> {
            MultiSelect(
                field,
                modifier,
                label,
                updateString,
                helpHint,
                showHelp,
                enabled,
            )
        }

        "cronselect" -> {
            val updateGroupExpandValue = remember(field) {
                { b: Boolean -> groupExpandState[field.key] = b }
            }
            CronSelect(
                field,
                groupExpandState[field.key] ?: false,
                updateGroupExpandValue,
                modifier,
                label,
                updateString,
                helpHint,
                showHelp,
                enabled,
            )
        }

        else -> {
            Text("$label ${field.key} ${field.field.htmlType}")
        }
    }
}

@Composable
private fun CheckboxItem(
    itemData: FieldDynamicValue,
    updateBoolean: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    helpHint: String,
    showHelp: () -> Unit = {},
    enabled: Boolean = true,
    updateWorkTypeStatus: (WorkTypeStatus) -> Unit = {},
) {
    val isNewCase = LocalCaseEditor.current.isNewCase
    val isChecked = itemData.dynamicValue.valueBoolean
    val isActiveWorkType = isChecked && itemData.isWorkTypeGroup

    val trailingContent: (@Composable () -> Unit)? = if (!isNewCase && isActiveWorkType) {
        @Composable {
            WorkTypeStatusDropdown(
                itemData.workTypeStatus,
                updateWorkTypeStatus,
                true,
            )
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
        updateBoolean,
        enabled = enabled,
        enableToggle = !isActiveWorkType,
        spaceTrailingContent = itemData.isWorkTypeGroup,
        trailingContent = trailingContent,
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

        val hasFocus = glassState.isGlassBroken && glassState.takeBrokenGlassFocus()

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
            CrisisCleanupIconButton(
                modifier = Modifier
                    .listRowItemStartPadding()
                    .centerAlignTextFieldLabelOffset(),
                imageVector = CrisisCleanupIcons.Edit,
                contentDescription = breakGlassHint,
                onClick = onBreakGlass,
                enabled = enabled,
            )
        }

        if (hasHelp) {
            HelpAction(helpHint, showHelp)
        }
    }
}

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

        // TODO Done (and onDone) if last in list.
        CrisisCleanupTextArea(
            text = itemData.dynamicValue.valueString,
            onTextChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            imeAction = ImeAction.Next,
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
        Column(
            Modifier
                .clickable(
                    onClick = { showDropdown = !showDropdown },
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
                var tint = LocalContentColor.current
                if (!enabled) {
                    tint = tint.disabledAlpha()
                }
                val description =
                    LocalAppTranslator.current("formLabels.select_option_for")
                        .replace("{field}", label)
                Icon(
                    imageVector = CrisisCleanupIcons.UnfoldMore,
                    contentDescription = description,
                    tint = tint,
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

        if (itemData.selectOptions.isNotEmpty()) {
            val onSelect = { key: String ->
                onChange(key)
                showDropdown = false
            }
            DropdownMenu(
                modifier = Modifier
                    .width(with(LocalDensity.current) {
                        contentWidth.width.toDp().minus(listItemDropdownMenuOffset.x.times(2))
                    }),
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                offset = listItemDropdownMenuOffset,
            ) {
                DropdownItems(
                    itemData.selectOptions,
                ) {
                    onSelect(it)
                }
            }
        }
    }
}

@Composable
private fun DropdownItems(
    options: Map<String, String>,
    onSelect: (String) -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier.optionItemHeight(),
        text = {
            Text(
                "",
                style = LocalFontStyles.current.header4,
            )
        },
        onClick = { onSelect("") },
    )
    for (option in options) {
        key(option.key) {
            DropdownMenuItem(
                modifier = Modifier.optionItemHeight(),
                text = {
                    Text(
                        option.value,
                        style = LocalFontStyles.current.header4,
                    )
                },
                onClick = { onSelect(option.key) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiSelect(
    itemData: FieldDynamicValue,
    modifier: Modifier = Modifier,
    label: String = "",
    onChange: (String) -> Unit = {},
    helpHint: String,
    showHelp: () -> Unit = {},
    enabled: Boolean = true,
) {
    val selected = itemData.dynamicValue.valueString.split(",").toSet()
    Column(modifier) {
        Row(
            Modifier.listItemVerticalPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label)
            if (itemData.field.help.isNotBlank()) {
                HelpAction(helpHint, showHelp)
            }
        }
        FlowRow(
            horizontalArrangement = listItemSpacedBy,
        ) {
            itemData.selectOptions.forEach { (key, option) ->
                val isSelected = selected.contains(key)
                CrisisCleanupFilterChip(
                    selected = isSelected,
                    onClick = {
                        val selectedOptions = if (isSelected) {
                            selected.filter { it != key }
                        } else {
                            listOf(itemData.dynamicValue.valueString, key)
                        }
                        onChange(selectedOptions.joinToString(","))
                    },
                    label = { Text(option) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun CronSelect(
    itemData: FieldDynamicValue,
    isInputExpanded: Boolean,
    expandFrequencyInput: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    updateFrequency: (String) -> Unit = {},
    helpHint: String,
    showHelp: () -> Unit = {},
    enabled: Boolean = true,
) {
    val trailingContent: (@Composable () -> Unit)? =
        if (itemData.field.help.isNotBlank()) {
            {
                HelpAction(helpHint, showHelp)
            }
        } else null
    val defaultRrule = itemData.field.recurDefault.ifBlank { FallbackRrule }
    val rRuleIn = itemData.dynamicValue.valueString
    val showFrequencyInput = isInputExpanded || (rRuleIn.isNotBlank() && defaultRrule != rRuleIn)
    CrisisCleanupTextCheckbox(
        modifier.listCheckboxAlignStartOffset(),
        showFrequencyInput,
        0,
        label,
        { expandFrequencyInput(!itemData.dynamicValue.valueBoolean) },
        expandFrequencyInput,
        enabled = enabled && !showFrequencyInput,
        trailingContent = trailingContent,
    )

    if (showFrequencyInput) {
        FrequencyDailyWeeklyViews(
            modifier,
            rRuleIn,
            defaultRrule,
            enabled,
            updateFrequency,
        )
    }
}
