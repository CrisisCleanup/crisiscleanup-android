package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemPadding

@Composable
fun ListOptionsDropdown(
    text: String,
    isEditable: Boolean,
    onToggleDropdown: () -> Unit,
    modifier: Modifier = Modifier,
    dropdownIconContentDescription: String? = null,
    optionsContent: @Composable (Size) -> Unit = { _ -> },
) {
    var contentSize by remember { mutableStateOf(Size.Zero) }
    Row(
        modifier
            .actionHeight()
            .roundedOutline(radius = 3.dp)
            .clickable(
                onClick = onToggleDropdown,
                enabled = isEditable,
            )
            .listItemPadding()
            .onGloballyPositioned {
                contentSize = it.size.toSize()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text)
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = CrisisCleanupIcons.ExpandAll,
            contentDescription = dropdownIconContentDescription,
        )
    }

    optionsContent(contentSize)
}
