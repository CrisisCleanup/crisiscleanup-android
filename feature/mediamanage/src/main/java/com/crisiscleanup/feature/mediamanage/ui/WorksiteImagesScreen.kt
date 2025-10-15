package com.crisiscleanup.feature.mediamanage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.crisiscleanup.core.designsystem.component.MultiImageScreen
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.feature.mediamanage.WorksiteImagesViewModel
import kotlin.math.min
import com.crisiscleanup.core.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorksiteImagesRoute(
    onBack: () -> Unit = {},
    viewModel: WorksiteImagesViewModel = hiltViewModel(),
) {
    var showPhotosGrid by rememberSaveable { mutableStateOf(false) }
    val hidePhotosGridOnBack = remember(viewModel) {
        {
            if (showPhotosGrid) {
                showPhotosGrid = false
            } else {
                onBack()
            }
        }
    }
    BackHandler {
        hidePhotosGridOnBack()
    }

    var isOverlayActions by rememberSaveable { mutableStateOf(true) }
    val toggleActions = remember(viewModel) { { isOverlayActions = !isOverlayActions } }

    if (viewModel.isDeletedImages) {
        onBack()
    } else {
        val screenTitle = viewModel.screenTitle

        val imageIds by viewModel.imageIds.collectAsStateWithLifecycle()
        val viewState by viewModel.viewState.collectAsStateWithLifecycle()
        val selectedImage by viewModel.selectedImageData.collectAsStateWithLifecycle()
        val enableRotateActions by viewModel.enableRotate.collectAsStateWithLifecycle()
        val enableDeleteAction by viewModel.enableDelete.collectAsStateWithLifecycle()
        val isExistingImage = selectedImage.id > 0
        MultiImageScreen(
            screenTitle,
            viewState,
            imageIds,
            isDeletable = enableDeleteAction,
            onBack = hidePhotosGridOnBack,
            isFullscreenMode = !showPhotosGrid,
            isOverlayActions = isOverlayActions,
            toggleActions = toggleActions,
            pageToIndex = viewModel.selectedImageIndex,
            onImageIndexChange = viewModel::onChangeImageIndex,
            onDeleteImage = viewModel::deleteImage,
            showRotateActions = isExistingImage,
            enableRotateActions = enableRotateActions,
            imageRotation = selectedImage.rotateDegrees,
            rotateImage = viewModel::rotateImage,
            showGridAction = imageIds.size > 1,
            onShowPhotos = {
                showPhotosGrid = true
            },
        )

        AnimatedVisibility(
            visible = showPhotosGrid,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            var contentSize by remember { mutableStateOf(Size.Zero) }
            val navigationContent = @Composable { TopBarBackAction(action = hidePhotosGridOnBack) }
            // TODO Fill status bar space on OS29/30
            Column(
                Modifier
                    .background(Color.White)
                    .fillMaxSize()
                    .onGloballyPositioned {
                        contentSize = it.size.toSize()
                    },
            ) {
                CenterAlignedTopAppBar(
                    title = { Text(screenTitle) },
                    navigationIcon = navigationContent,
                    actions = {},
                )

                val caseImages by viewModel.caseImages.collectAsStateWithLifecycle()
                val itemSpacing = 1.dp
                val density = LocalDensity.current
                val itemSize = remember(contentSize) {
                    with(density) {
                        val minSize = min(contentSize.width, contentSize.height)
                        val size = if (minSize > 1) (minSize * 0.33f).toDp() else 128.dp
                        size.coerceIn(64.dp, 128.dp)
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = itemSize),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    items(caseImages.size) { index ->
                        val image = caseImages[index]
                        AsyncImage(
                            model = image.thumbnailUri.ifBlank { image.imageUri },
                            modifier = Modifier
                                .size(itemSize)
                                .clickable {
                                    viewModel.onOpenImage(index)
                                    showPhotosGrid = false
                                },
                            placeholder = painterResource(commonR.drawable.cc_grayscale_pin),
                            contentDescription = image.title,
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}
