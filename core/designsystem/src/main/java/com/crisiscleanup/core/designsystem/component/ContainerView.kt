package com.crisiscleanup.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.unfocusedBorderColor

fun Modifier.roundedOutline(
    width: Dp = 1.dp,
    radius: Dp = 4.dp,
    // TODO Common color
    color: Color = unfocusedBorderColor,
) = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(width = width.toPx()),
        cornerRadius = CornerRadius(radius.toPx()),
    )
}
