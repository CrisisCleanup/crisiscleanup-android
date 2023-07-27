package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.cardContainerColor
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = cardContainerColor,
    cornerRound: Dp = 4.dp,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRound),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = elevation,
    ) {
        content()
    }
}

@Composable
fun CardSurface(
    containerColor: Color = cardContainerColor,
    cornerRound: Dp = 4.dp,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    CardSurface(
        Modifier
            .listItemHorizontalPadding()
            .fillMaxWidth(),
        containerColor,
        cornerRound,
        elevation,
        content,
    )
}
