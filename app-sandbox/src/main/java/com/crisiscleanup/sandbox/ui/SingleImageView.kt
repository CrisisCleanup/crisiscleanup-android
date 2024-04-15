package com.crisiscleanup.sandbox.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.ViewImageScreen
import com.crisiscleanup.sandbox.SingleImageViewModel

@Composable
fun SingleImageRoute() {
    SingleImageView()
}

@Composable
private fun SingleImageView(
    viewModel: SingleImageViewModel = hiltViewModel(),
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

    val screenTitle = "image"
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val isDeletable = false
    val imageRotation by viewModel.imageRotation.collectAsStateWithLifecycle()
    ViewImageScreen(
        screenTitle,
        viewState,
        isDeletable,
        onBack = onBackRestoreFullscreen,
        isFullscreenMode = isFullscreenMode,
        isOverlayActions = isOverlayActions,
        toggleActions = toggleActions,
        imageRotation = imageRotation,
        rotateImage = viewModel::rotateImage,
    )
}
