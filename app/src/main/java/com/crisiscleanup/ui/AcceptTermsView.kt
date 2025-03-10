package com.crisiscleanup.ui

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.LinkifyHtmlText
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AcceptTermsView(
    termsOfServiceUrl: String,
    privacyPolicyUrl: String,
    isLoading: Boolean,
    isAcceptingTerms: Boolean,
    setAcceptingTerms: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onRejectTerms: () -> Unit = {},
    onAcceptTerms: () -> Unit = {},
    errorMessage: String = "",
) {
    val enable = !isLoading

    val t = LocalAppTranslator.current

    var showRejectTermsDialog by remember { mutableStateOf((false)) }

    Box {
        Column(modifier) {
            Text(
                t("termsConditionsModal.must_agree_to_use_ccu"),
                // TODO Common dimensions
                Modifier
                    .padding(16.dp)
                    .testTag("acceptTermsAgreeAgreementText"),
            )

            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .testTag("acceptTermsAgreement"),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        loadUrl(termsOfServiceUrl)
                    }
                },
            )

            if (errorMessage.isNotBlank()) {
                if (errorMessage.contains("/>")) {
                    LinkifyHtmlText(
                        errorMessage,
                        listItemModifier.testTag("acceptTermsErrorMessage"),
                        // TODO Apply color to text view
                        // color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        errorMessage,
                        listItemModifier.testTag("acceptTermsErrorMessage"),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Row(
                listItemModifier,
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = isAcceptingTerms,
                    onCheckedChange = setAcceptingTerms,
                    modifier = Modifier.testTag("acceptTermsAcceptToggle"),
                    enabled = enable,
                )
                val acceptText = t("termsConditionsModal.accept_toc_privacy")
                    .replace("{terms_url}", termsOfServiceUrl)
                    .replace("{privacy_url}", privacyPolicyUrl)
                LinkifyHtmlText(
                    acceptText,
                    Modifier.testTag("acceptTermsAcceptText"),
                )
            }

            FlowRow(
                modifier = listItemModifier,
                horizontalArrangement = listItemSpacedBy,
                verticalArrangement = listItemSpacedBy,
                maxItemsInEachRow = 2,
            ) {
                val style = LocalFontStyles.current.header5
                BusyButton(
                    Modifier
                        .testTag("acceptTermsRejectAction")
                        .weight(1f),
                    text = t("actions.reject"),
                    enabled = enable,
                    onClick = { showRejectTermsDialog = true },
                    colors = cancelButtonColors(),
                    style = style,
                )
                BusyButton(
                    Modifier
                        .testTag("acceptTermsAcceptAction")
                        .weight(1f),
                    text = t("actions.accept"),
                    enabled = enable,
                    indicateBusy = isLoading,
                    onClick = onAcceptTerms,
                    style = style,
                )
            }
        }

        if (showRejectTermsDialog) {
            val closeRejectTermsDialog = { showRejectTermsDialog = false }
            ConfirmRejectTermsDialog(
                onConfirmReject = {
                    closeRejectTermsDialog()
                    onRejectTerms()
                },
                onCancel = closeRejectTermsDialog,
            )
        }
    }
}

@Composable
private fun ConfirmRejectTermsDialog(
    onConfirmReject: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val t = LocalAppTranslator.current
    CrisisCleanupAlertDialog(
        title = t("termsConditionsModal.are_you_sure"),
        textContent = {
            Text(
                t("termsConditionsModal.if_reject_cannot_use"),
                Modifier.testTag("rejectTermsConfirmText"),
            )
        },
        onDismissRequest = onCancel,
        dismissButton = {
            CrisisCleanupTextButton(
                text = t("actions.cancel"),
                modifier = Modifier.testTag("rejectTermsCancelAction"),
                onClick = onCancel,
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                text = t("actions.reject"),
                modifier = Modifier.testTag("rejectTermsConfirmRejectAction"),
                onClick = onConfirmReject,
            )
        },
    )
}
