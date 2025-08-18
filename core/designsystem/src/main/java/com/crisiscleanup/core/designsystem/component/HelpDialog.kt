package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@Composable
fun HelpRow(
    text: String,
    iconContentDescription: String,
    modifier: Modifier = Modifier,
    showHelp: () -> Unit = {},
    isBold: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier = Modifier.testTag("helpRowText_$text"),
            fontWeight = if (isBold) FontWeight.Bold else null,
        )
        HelpAction(iconContentDescription, showHelp)
    }
}

@Composable
fun HelpAction(
    helpHint: String,
    onShowHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CrisisCleanupIconButton(
        modifier = modifier,
        imageVector = CrisisCleanupIcons.Help,
        contentDescription = helpHint,
        onClick = onShowHelp,
    )
}

@Composable
fun HelpDialog(
    title: String,
    text: String,
    onClose: () -> Unit = {},
    hasHtml: Boolean = false,
) {
    CrisisCleanupAlertDialog(
        title = title,
        textContent = {
            if (hasHtml) {
                LinkifyHtmlText(text)
            } else {
                Text(text)
            }
        },
        onDismissRequest = onClose,
        confirmButton = {
            CrisisCleanupTextButton(
                text = LocalAppTranslator.current("actions.ok"),
                onClick = onClose,
            )
        },
    )
}

@Composable
fun WithHelpDialog(
    rememberKey: Any,
    helpTitle: String,
    helpText: String,
    hasHtml: Boolean = false,
    content: @Composable (() -> Unit) -> Unit,
) {
    var rememberHelpTitle by remember { mutableStateOf("") }
    var rememberHelpText by remember { mutableStateOf("") }
    val showHelp = remember(rememberKey) {
        {
            rememberHelpTitle = helpTitle
            rememberHelpText = helpText
        }
    }

    content(showHelp)

    if (rememberHelpText.isNotBlank()) {
        HelpDialog(
            title = rememberHelpTitle,
            text = rememberHelpText,
            onClose = { rememberHelpText = "" },
            hasHtml = hasHtml,
        )
    }
}
