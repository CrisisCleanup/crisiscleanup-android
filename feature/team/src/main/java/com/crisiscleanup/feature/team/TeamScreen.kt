package com.crisiscleanup.feature.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    Box(Modifier.fillMaxSize()) {
        Text(
            text = "Team",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}