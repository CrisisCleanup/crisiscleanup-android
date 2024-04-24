package com.crisiscleanup.feature.mediamanage.ui

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
import com.crisiscleanup.feature.mediamanage.WorksiteImagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewWorksiteImagesRoute(
    viewModel: WorksiteImagesViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
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

    // TODO If loaded and no images left (due to deletion) back out
    if (viewModel.isDeletedImages) {
        onBack()
    } else {
        val screenTitle = viewModel.screenTitle

        // TODO Multi images screen
        val imageIds by viewModel.imageIds.collectAsStateWithLifecycle()
        val viewState by viewModel.viewState.collectAsStateWithLifecycle()
        MultiImageScreen(
            screenTitle,
            viewState,
            imageIds,
            // TODO After carousel is working
            isDeletable = false,
            onBack = hidePhotosGridOnBack,
            isFullscreenMode = !showPhotosGrid,
            isOverlayActions = isOverlayActions,
            toggleActions = toggleActions,
            pageToIndex = viewModel.selectedImageIndex,
            onImageIndexChange = viewModel::onChangeImageIndex,
            // TODO After carousel is working
            imageRotation = 0,
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
}
