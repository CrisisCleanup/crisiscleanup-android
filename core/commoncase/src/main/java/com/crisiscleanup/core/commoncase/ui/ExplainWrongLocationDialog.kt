package com.crisiscleanup.core.commoncase.com.crisiscleanup.core.commoncase.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.attentionBackgroundColor

@Composable
fun ExplainWrongLocationDialog(
    rememberKey: Any,
) {
    var isWrongLocationDialogVisible by remember { mutableStateOf(false) }
    val showWrongLocationDialog =
        remember(rememberKey) { { isWrongLocationDialogVisible = true } }
    val hideWrongLocationDialog =
        remember(rememberKey) { { isWrongLocationDialogVisible = false } }

    CrisisCleanupIconButton(
        imageVector = CrisisCleanupIcons.Warning,
        contentDescription = LocalAppTranslator.current("flag.worksite_wrong_location_description"),
        tint = attentionBackgroundColor,
        onClick = showWrongLocationDialog,
    )

    if (isWrongLocationDialogVisible) {
        val t = LocalAppTranslator.current
        CrisisCleanupAlertDialog(
            title = t("flag.worksite_wrong_location"),
            text = t("flag.worksite_wrong_location_description"),
            onDismissRequest = hideWrongLocationDialog,
            confirmButton = {
                CrisisCleanupTextButton(
                    text = t("actions.ok"),
                    onClick = hideWrongLocationDialog,
                )
            },
        )
    }
}