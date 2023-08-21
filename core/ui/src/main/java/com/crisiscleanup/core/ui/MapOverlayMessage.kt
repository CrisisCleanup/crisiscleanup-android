package com.crisiscleanup.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.textMessagePadding

@Composable
fun BoxScope.MapOverlayMessage(
    message: String,
) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.BottomStart),
        visible = message.isNotBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            Modifier.padding(
                horizontal = 16.dp,
                vertical = 48.dp,
            ),
        ) {
            Text(
                text = message,
                modifier = Modifier.textMessagePadding(),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
