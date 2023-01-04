package com.crisiscleanup.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi


@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    DashboardScreen(
        modifier = modifier
    )
}

@Composable
internal fun DashboardScreen(
    modifier: Modifier = Modifier
) {
}