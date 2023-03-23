package com.crisiscleanup.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.BusyIndicatorFloatingTopCenter(
    isBusy: Boolean,
) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.TopCenter),
        visible = isBusy,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        CircularProgressIndicator(
            Modifier
                .wrapContentSize()
                .padding(96.dp)
                .size(24.dp)
        )
    }
}