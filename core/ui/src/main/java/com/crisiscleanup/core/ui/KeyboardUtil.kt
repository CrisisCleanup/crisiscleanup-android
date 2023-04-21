package com.crisiscleanup.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager

@Composable
private fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

@Composable
fun rememberCloseKeyboard(rememberKey: Any): () -> Unit {
    val isKeyboardOpen by keyboardAsState()
    val focusManager = LocalFocusManager.current
    return remember(rememberKey) {
        {
            if (isKeyboardOpen) {
                focusManager.clearFocus(true)
            }
        }
    }
}
