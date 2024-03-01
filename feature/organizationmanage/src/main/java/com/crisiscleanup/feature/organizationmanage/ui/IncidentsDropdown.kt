package com.crisiscleanup.feature.organizationmanage.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.Incident

@Composable
internal fun IncidentsDropdown(
    incidents: List<Incident>,
    contentSize: Size,
    showDropdown: Boolean,
    onSelect: (Incident) -> Unit,
    onHideDropdown: () -> Unit,
    isEditable: (Incident) -> Boolean = { true },
) {
    if (incidents.isNotEmpty()) {
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
            IncidentOptions(incidents, onSelect, isEditable)
        }
    }
}

@Composable
private fun IncidentOptions(
    incidents: List<Incident>,
    onSelect: (Incident) -> Unit,
    isEditable: (Incident) -> Boolean = { true },
) {
    for (incident in incidents) {
        key(incident.id) {
            DropdownMenuItem(
                text = {
                    Text(
                        incident.name,
                        style = LocalFontStyles.current.header4,
                    )
                },
                onClick = { onSelect(incident) },
                modifier = Modifier.optionItemHeight(),
                enabled = isEditable(incident),
            )
        }
    }
}
