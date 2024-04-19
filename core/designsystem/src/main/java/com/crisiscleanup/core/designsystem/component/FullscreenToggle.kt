package com.crisiscleanup.core.designsystem.component

import android.app.Activity
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private fun toggleFullscreen(window: Window, enterFullscreen: Boolean) {
    with(WindowCompat.getInsetsController(window, window.decorView)) {
        systemBarsBehavior = if (enterFullscreen) {
            hide(WindowInsetsCompat.Type.systemBars())
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            show(WindowInsetsCompat.Type.systemBars())
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
}

@Composable
fun ConfigureFullscreenView() {
    (LocalContext.current as? Activity)?.window?.let { window ->
        DisposableEffect(Unit) {
            toggleFullscreen(window, true)
            onDispose {
                toggleFullscreen(window, false)
            }
        }
    }
}

@Composable
fun ExitFullscreenView() {
    (LocalContext.current as? Activity)?.window?.let { window ->
        toggleFullscreen(window, false)
    }
}
