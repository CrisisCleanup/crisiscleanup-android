package com.crisiscleanup.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.roundedOutline(
    width: Dp = 1.dp,
    radius: Dp = 4.dp,
) = drawBehind {
    drawRoundRect(
        // TODO Common color
        color = Color.Gray,
        style = Stroke(width = width.toPx()),
        cornerRadius = CornerRadius(radius.toPx()),
    )
}
