package com.crisiscleanup.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImageScreen(
    screenTitle: String,
    viewState: ViewImageViewState,
    imageIds: List<String>,
    isDeletable: Boolean,
    onBack: () -> Unit = {},
    isFullscreenMode: Boolean = false,
    isOverlayActions: Boolean = false,
    toggleActions: () -> Unit = {},
    onImageIndexChange: (Int) -> Unit = {},
    onDeleteImage: () -> Unit = {},
    imageRotation: Int = 0,
    rotateImage: (Boolean) -> Unit = {},
    showGridAction: Boolean = false,
    onShowPhotos: () -> Unit = {},
) {
    if (isFullscreenMode) {
        ConfigureFullscreenView()
    } else {
        ExitFullscreenView()
    }

    val currentImageId = (viewState as? ViewImageViewState.Image)?.id ?: ""
    val imageCount = remember(imageIds) { { imageIds.size } }

    var contentSize by remember { mutableStateOf(Size.Zero) }
    val isImageLoaded = viewState is ViewImageViewState.Image && contentSize.width > 0
    Box(
        Modifier
            .background(color = Color.Black)
            .fillMaxSize()
            .onGloballyPositioned {
                contentSize = it.size.toSize()
            },
    ) {
        val pagerState = rememberPagerState(
            pageCount = imageCount,
        )

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                onImageIndexChange(page)
            }
        }

        HorizontalPager(
            pagerState,
        ) { pagerIndex ->
            val isImageAtIndex = pagerIndex < imageIds.size &&
                currentImageId == imageIds[pagerIndex]
            if (isImageAtIndex) {
                CarouselImage(
                    contentSize,
                    viewState,
                    imageRotation,
                    onClickImage = toggleActions,
                )
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    SmallBusyIndicator(color = Color.White)
                }
            }
        }

        val errorMessage = (viewState as? ViewImageViewState.Error)?.message
        AnimatedImageTopBarError(
            isOverlayActions || !isImageLoaded,
            screenTitle,
            onBack,
            isDeletable,
            onDeleteImage,
            errorMessage,
        )

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = isOverlayActions || !isImageLoaded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ImageActionBar(
                rotateImage,
                showGridAction = showGridAction,
                onShowPhotos = onShowPhotos,
            )
        }
    }
}

@Composable
private fun BoxScope.CarouselImage(
    contentSize: Size,
    viewState: ViewImageViewState,
    imageRotation: Int = 0,
    onClickImage: () -> Unit = {},
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
                    onClickImage,
                    alwaysPan = false,
                )
            }
        }

        else -> {}
    }
}
