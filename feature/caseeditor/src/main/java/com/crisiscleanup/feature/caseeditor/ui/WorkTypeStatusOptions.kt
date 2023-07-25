package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignItemPaddingCounterOffset
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.statusUnknownColor
import com.crisiscleanup.core.model.data.WorkTypeStatus

@Composable
internal fun WorkTypeStatusDropdown(
    selectedStatus: WorkTypeStatus,
    onStatusChange: (WorkTypeStatus) -> Unit,
    applySpacing: Boolean = false,
) {
    val (isEditable, statusOptions) = LocalCaseEditor.current
    val enabled = isEditable && statusOptions.isNotEmpty()

    var showOptions by remember { mutableStateOf(false) }
    val toggleShowOptions = { showOptions = !showOptions }
    Box {
        val restingModifier = if (applySpacing)
            Modifier
                .listCheckboxAlignItemPaddingCounterOffset()
                .clickable(
                    enabled = enabled,
                    onClick = toggleShowOptions,
                )
                .listItemHeight()
                .listItemPadding()
        else
            Modifier
                .clickable(
                    enabled = enabled,
                    onClick = toggleShowOptions,
                )
                .listItemHeight()
        WorkTypeStatusOption(
            selectedStatus,
            restingModifier,
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
            ) {
                WorkTypeStatusOptions(
                    selectedStatus,
                    onSelect,
                    statusOptions,
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
) {
    val modifier = Modifier.optionItemHeight()
    for (option in statusOptions) {
        DropdownMenuItem(
            // TODO Change color of selected option
            modifier = modifier,
            text = { WorkTypeStatusOption(option) },
            onClick = { onSelect(option) },
        )
    }
}

@Composable
private fun WorkTypeStatusOption(
    status: WorkTypeStatus,
    modifier: Modifier = Modifier,
    showOpenIcon: Boolean = false,
    enabled: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedByHalf,
    ) {
        val translator = LocalAppTranslator.current.translator
        Surface(
            Modifier.size(16.dp),
            shape = CircleShape,
            color = statusOptionColors[status] ?: statusUnknownColor,
        ) {}
        Text(
            translator(status.literal),
            style = MaterialTheme.typography.bodySmall,
        )
        if (showOpenIcon) {
            var tint = LocalContentColor.current
            if (!enabled) {
                tint = tint.disabledAlpha()
            }
            Icon(
                imageVector = CrisisCleanupIcons.ArrowDropDown,
                contentDescription = translator("actions.update_status"),
                tint = tint,
            )
        }
    }
}
