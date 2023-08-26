package com.crisiscleanup.core.ui

import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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


// https://stackoverflow.com/questions/68847559/how-can-i-detect-keyboard-opening-and-closing-in-jetpack-compose
@Composable
fun rememberIsKeyboardOpen(): Boolean {
    var isKeyboardOpen by remember { mutableStateOf(false) }
    val view = LocalView.current
    val viewTreeObserver = view.viewTreeObserver
    DisposableEffect(viewTreeObserver) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            isKeyboardOpen = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    return isKeyboardOpen
}
