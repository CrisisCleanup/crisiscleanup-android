package com.crisiscleanup.feature.mediamanage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.ViewImageScreen
import com.crisiscleanup.feature.mediamanage.ViewImageViewModel

@Composable
internal fun ViewImageRoute(
    viewModel: ViewImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    var isFullscreenMode by remember { mutableStateOf(true) }
    var isOverlayActions by remember { mutableStateOf(true) }
    val toggleActions = remember(viewModel) { { isOverlayActions = !isOverlayActions } }

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
        val screenTitle = viewModel.translate(viewModel.screenTitle)
        val viewState by viewModel.viewState.collectAsStateWithLifecycle()
        val isDeletable by viewModel.isImageDeletable.collectAsStateWithLifecycle()
        val imageRotation by viewModel.imageRotation.collectAsStateWithLifecycle()
        ViewImageScreen(
            screenTitle,
            viewState,
            isDeletable,
            onBack = onBackRestoreFullscreen,
            onDeleteImage = viewModel::deleteImage,
            isFullscreenMode = isFullscreenMode,
            isOverlayActions = isOverlayActions,
            toggleActions = toggleActions,
            imageRotation = imageRotation,
            rotateImage = viewModel::rotateImage,
        )
    }
}
