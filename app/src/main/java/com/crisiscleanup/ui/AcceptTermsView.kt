package com.crisiscleanup.ui

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.LinkifyUrlText
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.primaryRedColor


@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AcceptTermsView(
    termsOfServiceUrl: String,
    isLoading: Boolean,
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
                t("~~Using the Crisis Cleanup mobile app requires agreement with the following terms."),
                // TODO Common dimensions
                Modifier.padding(16.dp),
            )

            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        loadUrl(termsOfServiceUrl)
                    }
                },
            )

            // TODO Account for no internet access
            if (errorMessage.isNotBlank()) {
                Text(
                    errorMessage,
                    listItemModifier,
                    color = primaryRedColor,
                )
            }

            val acceptText = t("~~Do you accept the terms of service from {terms_url} ?")
                .replace("{terms_url}", termsOfServiceUrl)
            LinkifyUrlText(
                acceptText,
                listItemModifier,
            )
            FlowRow(
                modifier = listItemModifier,
                horizontalArrangement = listItemSpacedBy,
                verticalArrangement = listItemSpacedBy,
                maxItemsInEachRow = 2,
            ) {
                val style = LocalFontStyles.current.header5
                BusyButton(
                    Modifier
                        .testTag("caseEditCancelBtn")
                        .weight(1f),
                    text = t("actions.reject"),
                    enabled = enable,
                    onClick = { showRejectTermsDialog = true },
                    colors = cancelButtonColors(),
                    style = style,
                )
                BusyButton(
                    Modifier
                        .testTag("caseEditSaveBtn")
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

private fun getChromeClient(
    printLogs: Boolean = false,
): WebChromeClient {
    return object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            if (!printLogs) {
                return false
            }

            consoleMessage?.let { message ->
                val logMessage = with(message) {
                    "${message()}. Line ${lineNumber()} of ${sourceId()}"
                }
                Log.d("web-view", logMessage)
            }
            return true
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
        title = t("~~Reveiw decision"),
        textContent = {
            Text(
                text = t("Rejecting the terms of service will log you out from the app. You will not be able to use the app unless you log back in and accept the terms of service."),
            )
        },
        onDismissRequest = onCancel,
        dismissButton = {
            CrisisCleanupTextButton(
                text = t("actions.cancel"),
                onClick = onCancel,
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                text = t("actions.reject"),
                onClick = onConfirmReject,
            )
        },
    )
}
