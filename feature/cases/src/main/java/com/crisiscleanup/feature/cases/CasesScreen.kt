package com.crisiscleanup.feature.cases

import androidx.compose.foundation.layout.*
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
    Box(Modifier.fillMaxSize()) {
        Text(
            text = "Cases",
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}