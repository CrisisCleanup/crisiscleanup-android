package com.crisiscleanup.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun rememberCloseKeyboard(rememberKey: Any): () -> Unit {
    val focusManager = LocalFocusManager.current
    // val isKeyboardOpen by rememberUpdatedState(WindowInsets.isImeVisible)
    return remember(rememberKey) {
        {
            // TODO Only clear when keyboard is open
            //      Currently state does not reflect visibility when inside remember
            // Log.w("keyboard", "Is open? $isKeyboardOpen")
            focusManager.clearFocus(true)
        }
    }
}
