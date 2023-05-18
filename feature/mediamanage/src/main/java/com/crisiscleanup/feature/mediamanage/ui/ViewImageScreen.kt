package com.crisiscleanup.feature.mediamanage.ui

import android.app.Activity
import android.util.DisplayMetrics
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.feature.mediamanage.ViewImageUiState
import com.crisiscleanup.feature.mediamanage.ViewImageViewModel
import kotlin.math.max
import kotlin.math.min
import com.crisiscleanup.core.designsystem.R as designSystemR

// From TopAppBarSmallTokens.kt
private val topBarHeight = 64.dp

@Composable
internal fun ViewImageRoute(
    viewModel: ViewImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isFullscreenMode by remember { mutableStateOf(true) }
    val isImageLoaded = uiState is ViewImageUiState.Image
    val isFullscreenImage = isFullscreenMode && isImageLoaded
    (LocalContext.current as? Activity)?.window?.let { window ->
        with(WindowCompat.getInsetsController(window, window.decorView)) {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreenImage) {
                hide(WindowInsetsCompat.Type.systemBars())
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val toggleFullscreen = remember(viewModel) { { isFullscreenMode = !isFullscreenMode } }

    val contentModifier = if (isFullscreenImage) Modifier else Modifier.systemBarsPadding()
    Column(
        modifier = contentModifier,
    ) {
        if (!isFullscreenImage) {
            TopBar(onBack = onBack)

            if (uiState is ViewImageUiState.Error) {
                val errorState = uiState as ViewImageUiState.Error
                Text(
                    errorState.message,
                    listItemModifier.systemBarsPadding(),
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .background(color = Color.Black)
                .fillMaxSize(),
        ) {
            when (uiState) {
                ViewImageUiState.Loading -> {
                    BusyIndicatorFloatingTopCenter(
                        true,
                        // TODO Common styles
                        color = Color.White,
                    )
                }

                is ViewImageUiState.Image -> {
                    val imageData = uiState as ViewImageUiState.Image

                    DynamicImageView(imageData, isFullscreenMode, toggleFullscreen)
                }

                else -> {}
            }

            // TODO Show controls animating in and out.
            //      Send changes back to view model for saving.
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    viewModel: ViewImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val deleteImage = remember(viewModel) { { viewModel.deleteImage() } }

    val navigationContent: (@Composable (() -> Unit)) =
        @Composable {
            // TODO Style and heights
            Row(
                Modifier
                    .clickable(onClick = onBack)
                    .padding(8.dp)
            ) {
                Text(
                    stringResource(designSystemR.string.back),
                    color = primaryBlueColor,
                )
            }
        }
    val isDeletable by viewModel.isImageDeletable.collectAsStateWithLifecycle()
    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (isDeletable) {
        {
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.Delete,
                contentDescription = "actions.delete",
                onClick = deleteImage,
                enabled = true,
            )
        }
    } else {
        {}
    }
    CenterAlignedTopAppBar(
        title = { Text(viewModel.translate(viewModel.screenTitle)) },
        navigationIcon = navigationContent,
        actions = actionsContent,
    )
}

private fun Float.snapToNearest(min: Float, max: Float) = if (this - min < max - this) min else max

private fun capPanOffset(
    imageSize: Pair<Int, Int>,
    scale: Float,
    screenSize: Pair<Int, Int>,
    offset: Offset,
): Offset {
    val scaledWidth = imageSize.first * scale
    val scaledHeight = imageSize.second * scale
    val deltaX = ((scaledWidth - screenSize.first) * 0.5f).coerceAtLeast(0f)
    val deltaY = ((scaledHeight - screenSize.second) * 0.5f).coerceAtLeast(0f)
    return Offset(
        x = offset.x.coerceIn(-deltaX, deltaX),
        y = offset.y.coerceIn(-deltaY, deltaY),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DynamicImageView(
    imageData: ViewImageUiState.Image,
    isFullscreen: Boolean,
    toggleFullscreen: () -> Unit = {},
) {
    var screenSizeInset by remember { mutableStateOf(Pair(0, 0)) }
    var screenSizeFull by remember { mutableStateOf(Pair(0, 0)) }
    var imageSize by remember { mutableStateOf(Pair(0, 0)) }

    var minScale by remember { mutableStateOf(1f) }
    var maxScale by remember { mutableStateOf(1f) }
    var fitScale by remember { mutableStateOf(1f) }
    var fillScale by remember { mutableStateOf(1f) }
    var fitScalePx by remember { mutableStateOf(1f) }

    var scale by remember { mutableStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }

    var wasFullscreen by remember { mutableStateOf(false) }

    val image = imageData.image
    val isDefaultDimensions = imageSize.first == 0 && image.width > 0
    if (isDefaultDimensions) {
        val configuration = LocalConfiguration.current
        imageSize = Pair(image.width, image.height)
        with(LocalDensity.current) {
            screenSizeInset = Pair(
                configuration.screenWidthDp.dp.roundToPx(),
                configuration.screenHeightDp.dp.minus(topBarHeight).roundToPx(),
            )

            (LocalContext.current as? Activity)?.let {
                val outMetrics = DisplayMetrics()
                screenSizeFull =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        val bounds = it.windowManager.currentWindowMetrics.bounds
                        Pair(bounds.width(), bounds.height())
                    } else {
                        val display = it.windowManager.defaultDisplay
                        display.getMetrics(outMetrics)
                        Pair(outMetrics.widthPixels, outMetrics.heightPixels)
                    }
            }
        }

        wasFullscreen = isFullscreen
    }

    val screenSize =
        if (isFullscreen && screenSizeFull.first > 0) screenSizeFull else screenSizeInset

    if (isFullscreen != wasFullscreen || isDefaultDimensions) {
        val normalizedWidthScale =
            if (imageSize.first > 0) screenSize.first.toFloat() / imageSize.first else 1f
        val normalizedHeightScale =
            if (imageSize.second > 0) screenSize.second.toFloat() / imageSize.second else 1f
        fitScale = min(normalizedWidthScale, normalizedHeightScale)
        fillScale = max(normalizedWidthScale, normalizedHeightScale)

        minScale = 1f
        maxScale = fillScale * 10f

        fitScalePx = fitScale
        fillScale = if (fitScale > fillScale) {
            if (fillScale > 0) fitScale / fillScale else 1f
        } else {
            if (fitScale > 0) fillScale / fitScale else 1f
        }

        fitScale = 1f
        scale = scale.snapToNearest(fitScale, fillScale)

        val trueScale = scale * fitScalePx
        translation = capPanOffset(imageSize, trueScale, screenSize, translation)

        wasFullscreen = isFullscreen
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
                            else -> scale.snapToNearest(fitScale, fillScale)
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
                    val trueScale = scale * fitScalePx
                    translation = capPanOffset(imageSize, trueScale, screenSize, translation)
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