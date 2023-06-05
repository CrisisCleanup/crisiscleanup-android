package com.crisiscleanup.feature.mediamanage.ui

import android.app.Activity
import android.util.DisplayMetrics
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.feature.mediamanage.ViewImageUiState
import com.crisiscleanup.feature.mediamanage.ViewImageViewModel
import kotlin.math.max
import kotlin.math.min

// From TopAppBarSmallTokens.kt
private val topBarHeight = 64.dp

@Composable
internal fun ViewImageRoute(
    viewModel: ViewImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    var isFullscreenMode by remember { mutableStateOf(false) }
    val toggleFullscreen = remember(viewModel) { { isFullscreenMode = !isFullscreenMode } }

    val onBackRestoreFullscreen = remember(viewModel) {
        {
            isFullscreenMode = false
            onBack()
        }
    }

    BackHandler {
        onBackRestoreFullscreen()
    }

    val isDeleted by viewModel.isDeleted.collectAsStateWithLifecycle()
    // TODO Deleting shows white screen temporarily.
    //      Design a better transition as photo displays over black screen with optional toolbar.
    if (isDeleted) {
        onBackRestoreFullscreen()
    } else {
        ViewImageScreen(
            onBack = onBackRestoreFullscreen,
            isFullscreenMode = isFullscreenMode,
            toggleFullscreen = toggleFullscreen,
        )
    }
}

@Composable
private fun ViewImageScreen(
    viewModel: ViewImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    isFullscreenMode: Boolean = false,
    toggleFullscreen: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                    val imageRotation by viewModel.imageRotation.collectAsStateWithLifecycle()
                    DynamicImageView(imageData, imageRotation, isFullscreenMode, toggleFullscreen)

                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        visible = !isFullscreenImage,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        ImageActionBar()
                    }
                }

                else -> {}
            }
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

    val navigationContent = @Composable { TopBarBackAction(action = onBack) }
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
    rotationDegrees: Int = 0,
    isFullscreen: Boolean,
    toggleFullscreen: () -> Unit = {},
) {
    var screenSizeInset by remember { mutableStateOf(Pair(0, 0)) }
    var screenSizeFull by remember { mutableStateOf(Pair(0, 0)) }
    var imageSize by remember { mutableStateOf(Pair(0, 0)) }

    var imageScalesRest by remember { mutableStateOf(RectangularScale()) }
    var imageScalesRotated by remember { mutableStateOf(RectangularScale()) }

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
    val isRotated = rotationDegrees % 180 != 0

    if (isFullscreen != wasFullscreen || isDefaultDimensions) {
        imageScalesRest = RectangularScale.getScales(imageSize, screenSize)
        imageScalesRotated = RectangularScale.getScales(imageSize, screenSize, true)

        val scales = if (isRotated) imageScalesRotated else imageScalesRest

        scale = scale.snapToNearest(scales.fitScale, scales.fillScale)

        val trueScale = scale * scales.fitScalePx
        translation = capPanOffset(imageSize, trueScale, screenSize, translation)

        wasFullscreen = isFullscreen
    }

    val imageScales = if (isRotated) imageScalesRotated else imageScalesRest

    Image(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = toggleFullscreen,
                onDoubleClick = {
                    with(imageScales) {
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
                    }
                },
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(imageScales.minScale, imageScales.maxScale)
                    translation = (translation + pan)
                    val trueScale = scale * imageScales.fitScalePx
                    translation = capPanOffset(imageSize, trueScale, screenSize, translation)
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = translation.x,
                translationY = translation.y,
                rotationZ = rotationDegrees.toFloat(),
            ),
        contentDescription = null,
        bitmap = imageData.image,
    )
}

@Composable
private fun ImageActionBar(
    viewModel: ViewImageViewModel = hiltViewModel(),
) {
    Surface(
        modifier = listItemModifier,
        // TODO Common dimensions
        shape = RoundedCornerShape(4.dp),
        // TODO Common colors
        color = Color.White,
    ) {
        Row(
            horizontalArrangement = listItemSpacedBy,
        ) {
            Spacer(Modifier.weight(1f))
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.RotateCcw,
                onClick = { viewModel.rotateImage(false) }
            )
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.RotateClockwise,
                onClick = { viewModel.rotateImage(true) }
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

private data class RectangularScale(
    val minScale: Float = 1f,
    val maxScale: Float = 1f,
    val fitScale: Float = 1f,
    val fillScale: Float = 1f,
    val fitScalePx: Float = 1f,
) {
    companion object {
        fun getScales(
            imageSize: Pair<Int, Int>,
            screenSize: Pair<Int, Int>,
            isRotated: Boolean = false,
        ): RectangularScale {
            val width: Int
            val height: Int
            val (screenWidth, screenHeight) = screenSize
            if (isRotated) {
                width = imageSize.second
                height = imageSize.first
            } else {
                width = imageSize.first
                height = imageSize.second
            }
            val normalizedWidthScale = if (width > 0) screenWidth.toFloat() / width else 1f
            val normalizedHeightScale = if (height > 0) screenHeight.toFloat() / height else 1f
            var fitScale = min(normalizedWidthScale, normalizedHeightScale)
            var fillScale = max(normalizedWidthScale, normalizedHeightScale)

            val minScale = 1f
            val maxScale = fillScale * 10f

            val fitScalePx = fitScale
            fillScale = if (fitScale > fillScale) {
                if (fillScale > 0) fitScale / fillScale else 1f
            } else {
                if (fitScale > 0) fillScale / fitScale else 1f
            }

            fitScale = 1f
            return RectangularScale(minScale, maxScale, fitScale, fillScale, fitScalePx)
        }
    }
}