package com.crisiscleanup.feature.cases

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi


@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun CasesRoute(
    modifier: Modifier = Modifier,
    viewModel: CasesViewModel = hiltViewModel()
) {
    CasesScreen(
        modifier = modifier
    )
}

@Composable
internal fun CasesScreen(
    modifier: Modifier = Modifier
) {
}