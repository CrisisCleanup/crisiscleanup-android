package com.crisiscleanup.feature.team

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi


@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun TeamRoute(
    modifier: Modifier = Modifier,
    viewModel: TeamViewModel = hiltViewModel()
) {
    TeamScreen(
        modifier = modifier
    )
}

@Composable
internal fun TeamScreen(
    modifier: Modifier = Modifier
) {
}