package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

private val errorMessageModifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)

@Composable
internal fun ErrorText(
    errorMessage: String,
) {
    if (errorMessage.isNotEmpty()) {
        Text(
            errorMessage,
            modifier = errorMessageModifier,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

internal val columnItemModifier = Modifier
    .fillMaxWidth()
    .padding(16.dp, 8.dp)

@Composable
fun rememberCloseKeyboard(rememberKey: Any): () -> Unit {
    val focusManager = LocalFocusManager.current
    return remember(rememberKey) { { focusManager.clearFocus(true) } }
}