package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@Composable
fun LeadingIconChip(
    text: String,
    onIconClick: () -> Unit,
    isEditable: Boolean,
    containerColor: Color,
    iconDescription: String? = null,
    contentTint: Color = LocalContentColor.current,
) {
    AssistChip(
        leadingIcon = {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 28.dp, minHeight = 40.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = isEditable,
                        onClick = onIconClick,
                        role = Role.Button,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.Clear,
                    contentDescription = iconDescription,
                    tint = contentTint,
                )
            }
        },
        label = {
            Text(
                text,
                Modifier
                    .padding(end = 2.dp)
                    .padding(top = 6.dp),
            )
        },
        shape = CircleShape,
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentTint,
        ),
        onClick = {},
    )
}