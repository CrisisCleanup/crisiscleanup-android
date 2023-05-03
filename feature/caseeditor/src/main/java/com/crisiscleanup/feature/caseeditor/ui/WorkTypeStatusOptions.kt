package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun WorkTypeStatusDropdown(
    selectedStatus: WorkTypeStatus,
    onStatusChange: (WorkTypeStatus) -> Unit,
    translate: (String) -> String = { s -> s },
    applySpacing: Boolean = false,
) {
    val (isEditable, statusOptions) = LocalCaseEditor.current
    val enabled = isEditable && statusOptions.isNotEmpty()

    var showOptions by remember { mutableStateOf(false) }
    val onShowOptions = { showOptions = true }
    Box {
        val restingModifier = if (applySpacing)
            Modifier
                .listCheckboxAlignItemPaddingCounterOffset()
                .clickable(
                    enabled = enabled,
                    onClick = onShowOptions,
                )
                .listItemHeight()
                .listItemPadding()
        else
            Modifier
                .clickable(
                    enabled = enabled,
                    onClick = onShowOptions,
                )
                .listItemHeight()
        WorkTypeStatusOption(
            selectedStatus,
            restingModifier,
            translate,
            true,
            enabled = enabled,
        )

        if (showOptions && enabled) {
            val onSelect = { selected: WorkTypeStatus ->
                if (selected != selectedStatus) {
                    onStatusChange(selected)
                }
                showOptions = false
            }
            DropdownMenu(
                expanded = true,
                onDismissRequest = { showOptions = false },
                offset = listItemDropdownMenuOffset,
                properties = PopupProperties(focusable = false),
            ) {
                WorkTypeStatusOptions(
                    selectedStatus,
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
    selectedStatus: WorkTypeStatus,
    onSelect: (WorkTypeStatus) -> Unit = {},
    statusOptions: List<WorkTypeStatus> = emptyList(),
    translate: (String) -> String = { s -> s },
) {
    val modifier = Modifier.optionItemHeight()
    for (option in statusOptions) {
        DropdownMenuItem(
            // TODO Change color of selected option
            modifier = modifier,
            text = { WorkTypeStatusOption(option, translate = translate) },
            onClick = { onSelect(option) },
        )
    }
}

@Composable
private fun WorkTypeStatusOption(
    status: WorkTypeStatus,
    modifier: Modifier = Modifier,
    translate: (String) -> String = { s -> s },
    showOpenIcon: Boolean = false,
    enabled: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedByHalf,
    ) {

        Surface(
            Modifier.size(16.dp),
            shape = CircleShape,
            color = statusOptionColors[status] ?: statusUnknownColor,
        ) {}
        Text(
            translate(status.literal),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (showOpenIcon) {
            var tint = LocalContentColor.current
            if (!enabled) {
                tint = tint.disabledAlpha()
            }
            Icon(
                imageVector = CrisisCleanupIcons.ArrowDropDown,
                contentDescription = stringResource(R.string.change_status),
                tint = tint,
            )
        }
    }
}
