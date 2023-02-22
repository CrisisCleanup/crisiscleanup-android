package com.crisiscleanup.feature.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun MenuRoute(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val expireTokenAction = if (viewModel.isDebug) {
        { viewModel.simulateTokenExpired() }
    } else null
    
    MenuScreen(
        modifier = modifier,
        versionText = viewModel.versionText,
        expireTokenAction = expireTokenAction,
    )
}

@Composable
internal fun MenuScreen(
    modifier: Modifier = Modifier,
    versionText: String,
    expireTokenAction: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                textAlign = TextAlign.Center,
                text = "Menu",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = versionText,
            )

            expireTokenAction?.let {
                TextButton(onClick = it) {
                    Text("Expire token")
                }
            }
        }
    }
}