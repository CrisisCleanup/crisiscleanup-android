package com.crisiscleanup.feature.organizationmanage.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset

@Composable
internal fun IncidentsDropdown(
    contentSize: Size,
    showDropdown: Boolean,
    onHideDropdown: () -> Unit,
    optionsContent: @Composable () -> Unit,
) {
    DropdownMenu(
        modifier = Modifier.width(
            with(LocalDensity.current) {
                contentSize.width.toDp().minus(listItemDropdownMenuOffset.x.times(2))
            },
        ),
        expanded = showDropdown,
        onDismissRequest = onHideDropdown,
        offset = listItemDropdownMenuOffset,
    ) {
        optionsContent()
    }
}
