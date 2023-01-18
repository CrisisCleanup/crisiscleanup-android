package com.crisiscleanup.feature.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun MenuRoute(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel()
) {
    MenuScreen(
        modifier = modifier
    )
}

@Composable
internal fun MenuScreen(
    modifier: Modifier = Modifier
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = "Menu",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}