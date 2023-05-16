package com.crisiscleanup.feature.mediamanage.ui

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.feature.mediamanage.ViewImageViewModel

@Composable
internal fun ViewImageRoute(
    viewModel: ViewImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    if (viewModel.imageUrl.isBlank()) {
        onBack()
    } else {
        Column {
            Text("Load image ${viewModel.imageUrl}")
            Spacer(modifier = Modifier.height(100.dp))
            var isFullscreenMode by remember { mutableStateOf(false) }
            CrisisCleanupTextButton(
                text = "Toggle",
                onClick = { isFullscreenMode = !isFullscreenMode }
            )
            (LocalContext.current as? Activity)?.window?.let { window ->
                with(WindowCompat.getInsetsController(window, window.decorView)) {
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (isFullscreenMode) {
                        hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }
    }
}