package com.crisiscleanup.feature.mediamanage.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.feature.mediamanage.ViewImageViewModel

@Composable
internal fun ViewImageRoute(
    viewModel: ViewImageViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    if (viewModel.imageUrl.isBlank()) {
        onBack()
    } else {
        Text("Load image ${viewModel.imageUrl}")
    }
}