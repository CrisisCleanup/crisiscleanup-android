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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

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
    pageToIndex: Int = -1,
    onImageIndexChange: (Int) -> Unit = {},
    onDeleteImage: (String) -> Unit = {},
    showRotateActions: Boolean = false,
    enableRotateActions: Boolean = false,
    imageRotation: Int = 0,
    rotateImage: (String, Boolean) -> Unit = { _, _ -> },
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

        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(pageToIndex, pagerState) {
            snapshotFlow { pagerState.pageCount }.collect { count ->
                if (pageToIndex in 0..<count) {
                    coroutineScope.launch {
                        pagerState.scrollToPage(pageToIndex)
                    }
                }
            }
        }

        HorizontalPager(
            pagerState,
        ) { pagerIndex ->
            val isImageAtIndex = pagerIndex < imageIds.size &&
                currentImageId == imageIds[pagerIndex]
            if (isImageAtIndex) {
                LoadingImage(
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
        val deleteSpecificImage = remember(currentImageId) { { onDeleteImage(currentImageId) } }
        AnimatedImageTopBarError(
            isOverlayActions || !isImageLoaded,
            screenTitle,
            onBack,
            isDeletable,
            deleteSpecificImage,
            errorMessage,
        )

        val rotateSpecificImage = remember(currentImageId) {
            { rotateClockwise: Boolean ->
                rotateImage(currentImageId, rotateClockwise)
            }
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = isOverlayActions || !isImageLoaded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ImageActionBar(
                showRotateActions = showRotateActions,
                enableRotateActions = enableRotateActions,
                rotateImage = rotateSpecificImage,
                showGridAction = showGridAction,
                onShowPhotos = onShowPhotos,
            )
        }
    }
}

@Composable
private fun BoxScope.LoadingImage(
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
