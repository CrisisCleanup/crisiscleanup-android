package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.LinkifyHtmlText

@Composable
fun OverClaimAlertDialog(
    closeDialog: () -> Unit,
) {
    val t = LocalAppTranslator.current
    CrisisCleanupAlertDialog(
        onDismissRequest = closeDialog,
        title = t("info.claiming_restricted_threshold_exceeded_title"),
        confirmButton = {
            CrisisCleanupTextButton(
                text = t("actions.ok"),
                onClick = closeDialog,
            )
        },
    ) {
        LinkifyHtmlText(t("info.claiming_restricted_threshold_exceeded"))
    }
}
