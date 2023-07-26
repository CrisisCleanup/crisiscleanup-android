package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles

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
    val titleComposable: @Composable () -> Unit = if (title.isBlank()) titleContent else {
        @Composable {
            Text(
                title,
                style = LocalFontStyles.current.header3,
            )
        }
    }
    val textComposable: @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge
        ) {
            if (text.isBlank()) textContent()
            else Text(text)
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