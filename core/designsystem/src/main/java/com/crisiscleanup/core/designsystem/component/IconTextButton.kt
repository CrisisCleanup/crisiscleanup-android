package com.crisiscleanup.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CrisisCleanupIconTextButton(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int = 0,
    imageVector: ImageVector? = null,
    label: String = "",
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    spacing: Dp = 0.dp,
    iconTint: Color = LocalContentColor.current,
    textColor: Color = Color.Unspecified,
) {
    Row(
        modifier = modifier.clickable(
            enabled = enabled,
            onClick = onClick,
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconResId != 0) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = label,
                tint = iconTint,
            )
        } else {
            imageVector?.let {
                Icon(
                    imageVector = it,
                    contentDescription = label,
                    tint = iconTint,
                )
            }
        }
        Text(
            label,
            Modifier.weight(1f),
            color = textColor,
        )
    }
}