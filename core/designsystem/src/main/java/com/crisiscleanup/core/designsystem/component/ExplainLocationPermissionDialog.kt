package com.crisiscleanup.core.designsystem.component

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.crisiscleanup.core.designsystem.LocalAppTranslator

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
                onClick = closeDialog,
                modifier = Modifier.testTag("ccuAlertDialogDismissBtn"),
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
                },
                modifier = Modifier.testTag("ccuAlertDialogConfirmBtn"),
            )
        },
    )
}

@Composable
fun ExplainLocationPermissionDialog(
    showDialog: Boolean,
    closeDialog: () -> Unit,
    explanation: String? = null,
) {
    if (showDialog) {
        val t = LocalAppTranslator.current
        val permissionExplanation = explanation ?: t("info.location_permission_explanation")
        OpenSettingsDialog(
            t("info.allow_access_to_location"),
            permissionExplanation,
            confirmText = t("info.app_settings"),
            dismissText = t("actions.close"),
            closeDialog = closeDialog,
        )
    }
}
