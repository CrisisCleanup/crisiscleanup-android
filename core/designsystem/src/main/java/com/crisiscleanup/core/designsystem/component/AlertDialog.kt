package com.crisiscleanup.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CrisisCleanupAlertDialog(
    tonalElevation: Dp = 0.dp,
    textContentColor: Color = LocalContentColor.current,
    onDismissRequest: () -> Unit = {},
    title: String = "",
    titleContent: @Composable () -> Unit = {},
    confirmButton: @Composable () -> Unit = { },
    dismissButton: (@Composable () -> Unit)? = null,
    textContent: @Composable () -> Unit = {},
) {
    val title: @Composable () -> Unit = if (title.isBlank()) titleContent else {
        @Composable { Text(title) }
    }
    AlertDialog(
        tonalElevation = tonalElevation,
        textContentColor = textContentColor,
        onDismissRequest = onDismissRequest,
        title = title,
        text = textContent,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}