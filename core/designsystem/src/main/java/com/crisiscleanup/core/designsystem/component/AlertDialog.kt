package com.crisiscleanup.core.designsystem.component

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.navigationContainerColor
import kotlinx.coroutines.delay

@Composable
fun CrisisCleanupAlertDialog(
    tonalElevation: Dp = 0.dp,
    textContentColor: Color = LocalContentColor.current,
    // TODO Common dimensions
    shape: Shape = RoundedCornerShape(8.dp),
    onDismissRequest: () -> Unit = {},
    title: String = "",
    titleContent: @Composable () -> Unit = {},
    confirmButton: @Composable () -> Unit = { },
    dismissButton: (@Composable () -> Unit)? = null,
    text: String = "",
    textContent: @Composable () -> Unit = {},
) {
    val titleComposable: @Composable () -> Unit = if (title.isBlank()) {
        titleContent
    } else {
        @Composable {
            Text(
                title,
                style = LocalFontStyles.current.header3,
            )
        }
    }
    val textComposable: @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge,
        ) {
            if (text.isBlank()) {
                textContent()
            } else {
                Text(text)
            }
        }
    }

    AlertDialog(
        tonalElevation = tonalElevation,
        textContentColor = textContentColor,
        shape = shape,
        onDismissRequest = onDismissRequest,
        title = titleComposable,
        text = textComposable,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

@Composable
fun BoxScope.TemporaryDialog(
    message: String,
    onDialogStartShow: () -> Unit = {},
    verticalOffset: Dp = 64.dp,
    visibleMillis: Long = 2000,
) {
    var showActionMessage by remember { mutableStateOf(false) }
    if (message.isNotBlank()) {
        LaunchedEffect(message) {
            onDialogStartShow()
            showActionMessage = true
            delay(visibleMillis)
            showActionMessage = false
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = showActionMessage,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        CardSurface(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                // TODO Common dimensions
                // Using vertical padding not y offset because it seems to render smoother
                .padding(horizontal = 8.dp, vertical = verticalOffset),
            containerColor = navigationContainerColor,
        ) {
            Text(
                text = message,
                // TODO Common dimensions
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
