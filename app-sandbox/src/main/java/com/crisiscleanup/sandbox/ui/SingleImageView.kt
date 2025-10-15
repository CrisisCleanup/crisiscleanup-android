package com.crisiscleanup.sandbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.ViewImageScreen
import com.crisiscleanup.sandbox.SingleImageViewModel

@Composable
fun SingleImageRoute(
    onBack: () -> Unit = {},
) {
    SingleImageView(onBack = onBack)
}

@Composable
private fun SingleImageView(
    viewModel: SingleImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    var isOverlayActions by rememberSaveable { mutableStateOf(true) }
    val toggleActions = remember(viewModel) { { isOverlayActions = !isOverlayActions } }

    val screenTitle = "image"
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val isDeletable = false
    val imageRotation by viewModel.imageRotation.collectAsStateWithLifecycle()
    ViewImageScreen(
        screenTitle,
        viewState,
        isDeletable,
        onBack = onBack,
        isFullscreenMode = true,
        isOverlayActions = isOverlayActions,
        toggleActions = toggleActions,
        imageRotation = imageRotation,
        rotateImage = viewModel::rotateImage,
    )
}
