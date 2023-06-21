package com.crisiscleanup.feature.caseeditor.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton

@Composable
fun OpenSettingsDialog(
    title: String,
    text: String,
    dismissText: String = "",
    confirmText: String = "",
    closeDialog: () -> Unit = {},
) {
    val context = LocalContext.current
    CrisisCleanupAlertDialog(
        title = title,
        text = text,
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
        val t = LocalAppTranslator.current.translator
        OpenSettingsDialog(
            t("info.allow_access_to_location"),
            t("info.location_permission_explanation"),
            confirmText = t("info.app_settings"),
            dismissText = closeText,
            closeDialog = closeDialog,
        )
    }
}