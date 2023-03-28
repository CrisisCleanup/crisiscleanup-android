package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@Composable
internal fun HelpAction(
    helpHint: String,
    showHelp: () -> Unit,
) {
    IconButton(onClick = showHelp) {
        Icon(
            imageVector = CrisisCleanupIcons.Help,
            contentDescription = helpHint,
        )
    }
}

@Composable
internal fun HelpDialog(
    title: String,
    text: String,
    onClose: () -> Unit = {},
) {
    AlertDialog(
        title = { Text(title) },
        text = { Text(text) },
        onDismissRequest = onClose,
        confirmButton = {
            CrisisCleanupTextButton(
                textResId = android.R.string.ok,
                onClick = onClose
            )
        },
    )
}
