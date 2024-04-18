package com.crisiscleanup.core.designsystem.component

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
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

    var contentSize by remember { mutableStateOf(Size.Zero) }
    Box(
        Modifier
            .background(color = Color.Black)
            .fillMaxSize()
            .onGloballyPositioned {
                contentSize = it.size.toSize()
            },
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
                if (contentSize.width > 0) {
                    DynamicImageView(
                        viewState,
                        contentSize,
                        imageRotation,
                        toggleActions,
                    )

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        visible = overlayActions,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        ImageActionBar(rotateImage)
                    }
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
            // TODO Fill status bar space on OS29/30
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
    imageSize: Size,
    scale: Float,
    fullSize: Size,
    offset: Offset,
): Offset {
    val scaledWidth = imageSize.width * scale
    val scaledHeight = imageSize.height * scale
    val deltaX = ((scaledWidth - fullSize.width) * 0.5f).coerceAtLeast(0f)
    val deltaY = ((scaledHeight - fullSize.height) * 0.5f).coerceAtLeast(0f)
    return Offset(
        x = offset.x.coerceIn(-deltaX, deltaX),
        y = offset.y.coerceIn(-deltaY, deltaY),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DynamicImageView(
    imageData: ViewImageViewState.Image,
    fullSize: Size,
    rotationDegrees: Int = 0,
    toggleActions: () -> Unit = {},
) {
    val image = imageData.image
    val imageSize = Size(image.width.toFloat(), image.height.toFloat())

    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }

    val isRotated = rotationDegrees % 180 != 0

    val imageScales = remember(imageSize, fullSize, isRotated) {
        RectangularScale.getScales(imageSize, fullSize, isRotated)
    }

    val orientedImageSize = remember(isRotated, imageSize) {
        if (isRotated) {
            Size(imageSize.height, imageSize.width)
        } else {
            imageSize
        }
    }

    LaunchedEffect(rotationDegrees) {
        scale = imageScales.fitScale
        translation = Offset.Zero
    }

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
                            scale = scale.snapToNearest(fitScale, fillScale)
                            translation = Offset.Zero
                        }
                    }
                },
            )
            .pointerInput(imageScales) {
                // TODO Zoom about centroid
                // TODO Determine velocity from pan and allow scroll drifting
                detectTransformGestures { centroid, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(imageScales.minScale, imageScales.maxScale)
                    translation = (translation + pan)
                    val trueScale = scale * imageScales.fitScalePx
                    translation = capPanOffset(
                        orientedImageSize,
                        trueScale,
                        fullSize,
                        translation,
                    )
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
            imageSize: Size,
            fullSize: Size,
            isRotated: Boolean = false,
        ): RectangularScale {
            val (fullWidth, fullHeight) = fullSize
            fun getFitFillScale(w: Float, h: Float): Pair<Float, Float> {
                val normalizedWidth = if (w > 0) fullWidth / w else 1f
                val normalizedHeight = if (h > 0) fullHeight / h else 1f
                val fitScale = min(normalizedWidth, normalizedHeight)
                val fillScale = max(normalizedWidth, normalizedHeight)
                return Pair(fitScale, fillScale)
            }

            var (fitScale, fillScale) = getFitFillScale(
                imageSize.width,
                imageSize.height,
            )

            val fitScalePx = fitScale

            if (isRotated) {
                val (rotatedFitScale, rotatedFillScale) = getFitFillScale(
                    imageSize.height,
                    imageSize.width,
                )
                if (fitScale > 0) {
                    fillScale = rotatedFillScale / fitScale
                    fitScale = rotatedFitScale / fitScale
                }
            } else {
                fillScale = if (fitScale > 0) fillScale / fitScale else 1f
                fitScale = 1f
            }

            val minScale = fitScale
            val maxScale = fillScale * 10f

            return RectangularScale(minScale, maxScale, fitScale, fillScale, fitScalePx)
        }
    }
}
