package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.theme.navigationContainerColor
import kotlinx.coroutines.delay

@Composable
internal fun BoxScope.CopiedToClipboard(
    clipboardContents: String,
    verticalOffset: Dp = 32.dp,
) {
    var showClipboardMessage by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    if (clipboardContents.isNotBlank()) {
        LaunchedEffect(clipboardContents) {
            clipboardManager.setText(AnnotatedString(clipboardContents))
            showClipboardMessage = true
            delay(2000)
            showClipboardMessage = false
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = showClipboardMessage,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        CardSurface(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                // TODO Common dimensions
                // Using vertical padding not y offset because it seems to render smoother
                .padding(horizontal = 4.dp, vertical = verticalOffset),
            containerColor = navigationContainerColor,
        ) {
            val translator = LocalAppTranslator.current.translator
            val copiedText = translator("info.copied_value")
                .replace("{copied_string}", clipboardContents)
            Text(
                text = copiedText,
                // TODO Common dimensions
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}