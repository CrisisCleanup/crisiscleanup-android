package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.TemporaryDialog

@Composable
internal fun BoxScope.CopiedToClipboard(
    clipboardContents: String,
) {
    val clipboardManager = LocalClipboardManager.current
    val translator = LocalAppTranslator.current
    val copiedText = if (clipboardContents.isBlank()) ""
    else translator("info.copied_value")
        .replace("{copied_string}", clipboardContents)
    TemporaryDialog(
        message = copiedText,
        onDialogStartShow = {
            clipboardManager.setText(AnnotatedString(clipboardContents))
        },
    )
}