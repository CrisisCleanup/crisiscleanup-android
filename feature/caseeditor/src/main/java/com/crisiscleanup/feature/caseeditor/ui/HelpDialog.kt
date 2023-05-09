package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.ui.LinkifyHtmlText

@Composable
internal fun HelpRow(
    text: String,
    iconContentDescription: String,
    modifier: Modifier = Modifier,
    showHelp: () -> Unit = {},
) {
    Row(
        modifier = modifier.listItemHorizontalPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text)
        HelpAction(iconContentDescription, showHelp)
    }
}

@Composable
internal fun HelpAction(
    helpHint: String,
    showHelp: () -> Unit,
) {
    IconButton(onClick = showHelp) {
        Icon(
            imageVector = CrisisCleanupIcons.Help,
            contentDescription = helpHint,
        )
    }
}

@Composable
internal fun HelpDialog(
    title: String,
    text: String,
    onClose: () -> Unit = {},
    hasHtml: Boolean = false,
    okText: String = "",
) {
    AlertDialog(
        title = { Text(title) },
        text = {
            if (hasHtml) {
                LinkifyHtmlText(text)
            } else {
                Text(text)
            }
        },
        onDismissRequest = onClose,
        confirmButton = {
            CrisisCleanupTextButton(
                text = okText,
                onClick = onClose,
            )
        },
    )
}

@Composable
internal fun WithHelpDialog(
    rememberKey: Any,
    helpTitle: String,
    helpText: String,
    hasHtml: Boolean = false,
    okText: String = "",
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
            okText = okText,
        )
    }
}