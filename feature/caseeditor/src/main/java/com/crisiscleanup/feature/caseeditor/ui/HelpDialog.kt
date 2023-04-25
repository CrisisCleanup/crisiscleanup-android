package com.crisiscleanup.feature.caseeditor.ui

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding

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
) {
    AlertDialog(
        title = { Text(title) },
        text = {
            if (hasHtml) {
                val context = LocalContext.current
                val linkifyTextView = remember { TextView(context) }
                AndroidView(
                    factory = { linkifyTextView },
                    update = { textView ->
                        textView.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        textView.movementMethod = LinkMovementMethod.getInstance()
                    },
                )
            } else {
                Text(text)
            }
        },
        onDismissRequest = onClose,
        confirmButton = {
            CrisisCleanupTextButton(
                textResId = android.R.string.ok,
                onClick = onClose,
            )
        },
    )
}
