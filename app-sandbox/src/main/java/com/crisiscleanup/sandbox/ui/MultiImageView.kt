package com.crisiscleanup.sandbox.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.MultiImageScreen
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.sandbox.MultiImageViewModel

@Composable
fun MultiImageRoute(
    onBack: () -> Unit = {},
) {
    MultiImageView(onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiImageView(
    onBack: () -> Unit = {},
    viewModel: MultiImageViewModel = hiltViewModel(),
) {
    var showPhotosGrid by remember { mutableStateOf(false) }
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

    val screenTitle = "images"
    val imagesData by viewModel.imagesData.collectAsStateWithLifecycle()
    val imageIds = imagesData.imageUrls
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    // Update accordingly in actual
    val isDeletable = false
    // Update according to each image's data
    val imageRotation by viewModel.imageRotation.collectAsStateWithLifecycle()
    MultiImageScreen(
        screenTitle,
        viewState,
        imageIds,
        isDeletable = isDeletable,
        onBack = hidePhotosGridOnBack,
        isFullscreenMode = !showPhotosGrid,
        isOverlayActions = isOverlayActions,
        toggleActions = toggleActions,
        onImageIndexChange = viewModel::onChangeImageIndex,
        showRotateActions = true,
        enableRotateActions = true,
        imageRotation = imageRotation,
        rotateImage = viewModel::rotateImage,
        showGridAction = imagesData.imageCount > 1,
        onShowPhotos = {
            showPhotosGrid = true
        },
    )

    AnimatedVisibility(
        visible = showPhotosGrid,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val navigationContent = @Composable { TopBarBackAction(action = hidePhotosGridOnBack) }
        // TODO Fill status bar space on OS29/30
        Column(
            Modifier
                .background(Color.White)
                .fillMaxSize(),
        ) {
            CenterAlignedTopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = navigationContent,
                actions = {},
            )

            // Show actual thumbnails with ability to open to selected photo on press
            Text("Show photos grid with decor")
        }
    }
}
