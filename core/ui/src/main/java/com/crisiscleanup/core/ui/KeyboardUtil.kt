package com.crisiscleanup.core.ui

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView

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

enum class ScreenKeyboardVisibility {
    Visible,
    NotVisible,
}

// From https://stackoverflow.com/questions/74539287/observing-soft-keyboard-visibility-opened-closed-jetpack-compose
@Composable
fun screenKeyboardVisibility(): State<ScreenKeyboardVisibility> {
    val keyboardState = remember { mutableStateOf(ScreenKeyboardVisibility.NotVisible) }
    val localView = LocalView.current
    DisposableEffect(localView) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            localView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = localView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            keyboardState.value = if (keypadHeight > screenHeight * 0.15) {
                ScreenKeyboardVisibility.Visible
            } else {
                ScreenKeyboardVisibility.NotVisible
            }
        }
        localView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            localView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return keyboardState
}
