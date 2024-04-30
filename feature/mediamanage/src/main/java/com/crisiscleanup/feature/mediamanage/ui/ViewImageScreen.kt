package com.crisiscleanup.feature.mediamanage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    var isOverlayActions by rememberSaveable { mutableStateOf(true) }
    val toggleActions = remember(viewModel) { { isOverlayActions = !isOverlayActions } }

    val isDeleted by viewModel.isDeleted.collectAsStateWithLifecycle()
    // TODO Deleting shows white screen temporarily.
    //      Design a better transition as photo displays over black screen with optional toolbar.
    if (isDeleted) {
        onBack()
    } else {
        val viewState by viewModel.viewState.collectAsStateWithLifecycle()
        val isDeletable by viewModel.isImageDeletable.collectAsStateWithLifecycle()
        val imageRotation by viewModel.imageRotation.collectAsStateWithLifecycle()
        ViewImageScreen(
            viewModel.screenTitle,
            viewState,
            isDeletable,
            onBack = onBack,
            onDeleteImage = viewModel::deleteImage,
            isFullscreenMode = true,
            isOverlayActions = isOverlayActions,
            toggleActions = toggleActions,
            imageRotation = imageRotation,
            rotateImage = viewModel::rotateImage,
        )
    }
}
