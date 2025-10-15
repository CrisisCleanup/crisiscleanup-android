package com.crisiscleanup.feature.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
internal fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    DashboardScreen(
        modifier = modifier,
    )
}

@Composable
internal fun DashboardScreen(
    modifier: Modifier = Modifier,
) {
    Box {
        Text(
            text = "Dash",
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
