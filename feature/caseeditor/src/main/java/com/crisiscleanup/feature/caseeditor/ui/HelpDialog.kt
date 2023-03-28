package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton

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
