package com.crisiscleanup.feature.caseeditor.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.feature.caseeditor.R

@Composable
fun OpenSettingsDialog(
    @StringRes titleResId: Int,
    @StringRes textResId: Int,
    @StringRes dismissResId: Int = R.string.close,
    @StringRes confirmResId: Int = R.string.app_settings,
    closeDialog: () -> Unit = {},
) {
    val context = LocalContext.current
    AlertDialog(
        title = {
            Text(text = stringResource(titleResId))
        },
        text = {
            Text(text = stringResource(textResId))
        },
        onDismissRequest = closeDialog,
        dismissButton = {
            CrisisCleanupTextButton(
                textResId = dismissResId,
                onClick = closeDialog
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                textResId = confirmResId,
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
) {
    if (showDialog) {
        OpenSettingsDialog(
            R.string.allow_location_permission,
            R.string.location_permission_explanation,
            closeDialog = closeDialog,
        )
    }
}