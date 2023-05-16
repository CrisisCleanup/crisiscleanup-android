package com.crisiscleanup.feature.mediamanage.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.feature.mediamanage.ViewImageUiState
import com.crisiscleanup.feature.mediamanage.ViewImageViewModel

@Composable
internal fun ViewImageRoute(
    viewModel: ViewImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    if (viewModel.imageUrl.isBlank()) {
        onBack()
    } else {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var isFullscreenMode by remember { mutableStateOf(true) }
        (LocalContext.current as? Activity)?.window?.let { window ->
            with(WindowCompat.getInsetsController(window, window.decorView)) {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isFullscreenMode && uiState !is ViewImageUiState.Error) {
                    hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        // TODO Use calculated min and max scales from view model
        val minScale = 0.5f
        val maxScale = 2f
        var scale by remember { mutableStateOf(1f) }
        var translation by remember { mutableStateOf(Offset(0f, 0f)) }
        fun calculateNewScale(k: Float) = (scale * k).coerceAtLeast(minScale).coerceAtMost(maxScale)
        Box(
            Modifier
                .clickable(
                    enabled = uiState is ViewImageUiState.Image,
                    onClick = { isFullscreenMode = !isFullscreenMode },
                )
                .systemBarsPadding()
                .background(color = Color.Black)
                .fillMaxSize(),
        ) {
            when (uiState) {
                ViewImageUiState.Loading -> {
                    BusyIndicatorFloatingTopCenter(true)
                }

                is ViewImageUiState.Image -> {
                    val imageData = uiState as ViewImageUiState.Image
                    Image(
                        modifier = Modifier
                            .fillMaxSize()
                            // TODO Double tap toggles between scale to 1 and scale to fit
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    // TODO Take centroid into account if necessary
                                    scale = calculateNewScale(zoom)
                                    // TODO Cap translation based on scale so that panning
                                    //      doesn't move image beyond screen bounds
                                    translation = translation.plus(pan)
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = translation.x,
                                translationY = translation.y
                            ),
                        contentDescription = null,
                        bitmap = imageData.image,
                    )
                }

                is ViewImageUiState.Error -> {
                    val errorState = uiState as ViewImageUiState.Error
                    // TODO Show error and message
                    Text(
                        errorState.message,
                        listItemModifier,
                    )
                }
            }
            // TODO Show decoration animating in and out
        }
    }
}