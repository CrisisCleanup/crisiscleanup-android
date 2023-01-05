package com.crisiscleanup.feature.authentication

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
internal fun AuthenticateRoute(
    modifier: Modifier = Modifier,
    viewModel: AuthenticationViewModel = hiltViewModel()
) {
    AuthenticateScreen(
        modifier = modifier
    )
}

@Composable
internal fun AuthenticateScreen(
    modifier: Modifier = Modifier
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = "Authenticate",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}