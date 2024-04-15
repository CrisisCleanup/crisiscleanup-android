package com.crisiscleanup.core.designsystem.component

import android.app.Activity
import android.util.DisplayMetrics
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.cardContainerColor
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import kotlin.math.max
import kotlin.math.min

sealed interface ViewImageViewState {
    data object Loading : ViewImageViewState
    data class Image(
        val image: ImageBitmap,
    ) : ViewImageViewState

    data class Error(
        val message: String,
    ) : ViewImageViewState
}

// From TopAppBarSmallTokens.kt
private val topBarHeight = 64.dp

@Composable
fun ViewImageScreen(
    screenTitle: String,
    viewState: ViewImageViewState,
    isDeletable: Boolean,
    onBack: () -> Unit = {},
    onDeleteImage: () -> Unit = {},
    isFullscreenMode: Boolean = false,
    isOverlayActions: Boolean = false,
    toggleActions: () -> Unit = {},
    imageRotation: Int = 0,
    rotateImage: (Boolean) -> Unit = {},
) {
    val isImageLoaded = viewState is ViewImageViewState.Image
    val overlayActions = isOverlayActions || !isImageLoaded

    if (isFullscreenMode) {
        ConfigureFullscreenView()
    }

    Box(
        Modifier
            .background(color = Color.Black)
            .fillMaxSize(),
    ) {
        when (viewState) {
            ViewImageViewState.Loading -> {
                BusyIndicatorFloatingTopCenter(
                    true,
                    // TODO Common styles
                    color = Color.White,
                )
            }

            is ViewImageViewState.Image -> {
                DynamicImageView(viewState, imageRotation, toggleActions)

                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    visible = overlayActions,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    ImageActionBar(rotateImage)
                }
            }

            else -> {}
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = overlayActions,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column {
                TopBar(
                    screenTitle,
                    onBack = onBack,
                    isDeletable = isDeletable,
                    onDeleteImage = onDeleteImage,
                )

                if (viewState is ViewImageViewState.Error) {
                    Text(
                        viewState.message,
                        listItemModifier.systemBarsPadding(),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    screenTitle: String,
    isDeletable: Boolean,
    onBack: () -> Unit = {},
    onDeleteImage: () -> Unit = {},
) {
    val navigationContent = @Composable { TopBarBackAction(action = onBack) }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (isDeletable) {
        {
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.Delete,
                contentDescription = "actions.delete",
                onClick = onDeleteImage,
                enabled = true,
            )
        }
    } else {
        {}
    }
    CenterAlignedTopAppBar(
        title = { Text(screenTitle) },
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
    imageData: ViewImageViewState.Image,
    rotationDegrees: Int = 0,
    toggleActions: () -> Unit = {},
) {
    var screenSizeInset by remember { mutableStateOf(Pair(0, 0)) }
    var screenSizeFull by remember { mutableStateOf(Pair(0, 0)) }
    var imageSize by remember { mutableStateOf(Pair(0, 0)) }

    var imageScalesRest by remember { mutableStateOf(RectangularScale()) }
    var imageScalesRotated by remember { mutableStateOf(RectangularScale()) }

    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }

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
    }

    val screenSize = screenSizeFull
    val isRotated = rotationDegrees % 180 != 0

    if (isDefaultDimensions) {
        imageScalesRest = RectangularScale.getScales(imageSize, screenSize)
        imageScalesRotated = RectangularScale.getScales(imageSize, screenSize, true)

        val scales = if (isRotated) imageScalesRotated else imageScalesRest

        scale = scale.snapToNearest(scales.fitScale, scales.fillScale)

        val trueScale = scale * scales.fitScalePx
        translation = capPanOffset(imageSize, trueScale, screenSize, translation)
    }

    val imageScales = if (isRotated) imageScalesRotated else imageScalesRest

    Image(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = toggleActions,
                onDoubleClick = {
                    with(imageScales) {
                        if (translation == Offset.Zero) {
                            scale = when (scale) {
                                fitScale -> fillScale
                                fillScale -> fitScale
                                else -> scale.snapToNearest(fitScale, fillScale)
                            }
                        } else {
                            scale = if (scale > fillScale) {
                                fillScale
                            } else {
                                fitScale
                            }
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
    rotateImage: (Boolean) -> Unit,
) {
    Surface(
        modifier = listItemModifier,
        // TODO Common dimensions
        shape = RoundedCornerShape(4.dp),
        color = cardContainerColor,
    ) {
        Row(
            horizontalArrangement = listItemSpacedBy,
        ) {
            Spacer(Modifier.weight(1f))
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.RotateCcw,
                onClick = { rotateImage(false) },
            )
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.RotateClockwise,
                onClick = { rotateImage(true) },
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
