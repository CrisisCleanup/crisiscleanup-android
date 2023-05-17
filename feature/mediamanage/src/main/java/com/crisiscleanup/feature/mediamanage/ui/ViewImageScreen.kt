package com.crisiscleanup.feature.mediamanage.ui

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.feature.mediamanage.ViewImageUiState
import com.crisiscleanup.feature.mediamanage.ViewImageViewModel
import kotlin.math.max
import kotlin.math.min

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
                if (isFullscreenMode && uiState is ViewImageUiState.Image) {
                    hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        val toggleFullscreen = remember(viewModel) { { isFullscreenMode = !isFullscreenMode } }

        val boxModifier = if (isFullscreenMode) Modifier
        else Modifier.systemBarsPadding()
        Box(
            boxModifier
                .background(color = Color.Black)
                .fillMaxSize(),
        ) {
            when (uiState) {
                ViewImageUiState.Loading -> {
                    BusyIndicatorFloatingTopCenter(true)
                }

                is ViewImageUiState.Image -> {
                    val imageData = uiState as ViewImageUiState.Image

                    DynamicImageView(imageData, toggleFullscreen)
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
            // TODO Show decoration and controls animating in and out.
            //      Signal back to view model to save.
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DynamicImageView(
    imageData: ViewImageUiState.Image,
    toggleFullscreen: () -> Unit = {},
) {

    var screenWidth by remember { mutableStateOf(0) }
    var screenHeight by remember { mutableStateOf(0) }
    var width by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(0) }

    var minScale by remember { mutableStateOf(1f) }
    var maxScale by remember { mutableStateOf(1f) }
    var fitScale by remember { mutableStateOf(1f) }
    var fillScale by remember { mutableStateOf(1f) }

    var scale by remember { mutableStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }

    val image = imageData.image
    if (width == 0 && image.width > 0) {
        val configuration = LocalConfiguration.current
        width = image.width
        height = image.height
        with(LocalDensity.current) {
            // TODO These account for insets.
            //      Use the correct value if in fullscreen mode.
            screenWidth = configuration.screenWidthDp.dp.roundToPx()
            screenHeight = configuration.screenHeightDp.dp.roundToPx()
        }

        val normalizedWidthScale =
            if (width > 0) screenWidth.toFloat() / width else 1f
        val normalizedHeightScale =
            if (height > 0) screenHeight.toFloat() / height else 1f
        fitScale = min(normalizedWidthScale, normalizedHeightScale)
        fillScale = max(normalizedWidthScale, normalizedHeightScale)

        minScale = 1f
        maxScale = fillScale * 10f

        fillScale = if (fitScale > fillScale) {
            if (fillScale > 0) fitScale / fillScale else 1f
        } else {
            if (fitScale > 0) fillScale / fitScale else 1f
        }

        fitScale = 1f
        scale = fitScale
    }

    Image(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = toggleFullscreen,
                onDoubleClick = {
                    if (translation == Offset.Zero) {
                        scale = when (scale) {
                            fitScale -> fillScale
                            fillScale -> fitScale
                            else -> {
                                if (fillScale - scale < scale - fitScale) fillScale
                                else fitScale
                            }
                        }
                    } else {
                        scale = if (scale > fillScale) fillScale
                        else fitScale
                        translation = Offset.Zero
                    }
                },
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(minScale, maxScale)
                    translation = (translation + pan)
                    // TODO Disallow translation from taking image offscreen
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = translation.x,
                translationY = translation.y,
            ),
        contentDescription = null,
        bitmap = imageData.image,
    )
}