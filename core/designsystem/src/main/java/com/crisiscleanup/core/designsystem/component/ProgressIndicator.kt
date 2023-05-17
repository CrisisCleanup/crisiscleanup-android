package com.crisiscleanup.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.BusyIndicatorFloatingTopCenter(
    isBusy: Boolean,
    color: Color = ProgressIndicatorDefaults.circularColor,
) {
    AnimatedBusyIndicator(
        isBusy,
        Modifier.align(Alignment.TopCenter),
        color = color,
    )
}

@Composable
fun AnimatedBusyIndicator(
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = 96.dp,
    size: Dp = 24.dp,
    color: Color = ProgressIndicatorDefaults.circularColor,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isBusy,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        CircularProgressIndicator(
            Modifier
                .wrapContentSize()
                .padding(padding)
                .size(size),
            color = color,
        )
    }
}