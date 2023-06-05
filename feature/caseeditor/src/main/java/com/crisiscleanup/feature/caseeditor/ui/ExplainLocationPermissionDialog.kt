package com.crisiscleanup.feature.caseeditor.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.feature.caseeditor.R

@Composable
fun OpenSettingsDialog(
    title: String,
    text: String,
    dismissText: String = "",
    confirmText: String = "",
    closeDialog: () -> Unit = {},
) {
    val context = LocalContext.current
    AlertDialog(
        title = { Text(title) },
        text = { Text(text) },
        onDismissRequest = closeDialog,
        dismissButton = {
            CrisisCleanupTextButton(
                text = dismissText,
                onClick = closeDialog
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                text = confirmText,
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    ContextCompat.startActivity(context, intent, null)
                    closeDialog()
                }
            )
        },
    )
}

@Composable
fun ExplainLocationPermissionDialog(
    showDialog: Boolean,
    closeDialog: () -> Unit,
    closeText: String = "",
) {
    if (showDialog) {
        val translator = LocalAppTranslator.current.translator
        OpenSettingsDialog(
            translator("info.allow_access_to_location", R.string.allow_location_permission),
            translator(
                "info.explain_access_to_location_android",
                R.string.location_permission_explanation
            ),
            confirmText = translator("info.app_settings", R.string.app_settings),
            dismissText = closeText,
            closeDialog = closeDialog,
        )
    }
}