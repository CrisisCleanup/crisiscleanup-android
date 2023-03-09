package com.crisiscleanup.feature.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun MenuRoute(
    modifier: Modifier = Modifier,
) {
    MenuScreen(
        modifier = modifier,
    )
}

@Composable
internal fun MenuScreen(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val databaseText = viewModel.databaseVersionText

    val expireTokenAction = if (viewModel.isDebug) {
        remember(viewModel) { { viewModel.simulateTokenExpired() } }
    } else null

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = viewModel.versionText,
            )

            if (databaseText.isNotEmpty()) {
                Text(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = databaseText,
                )
            }

            expireTokenAction?.let {
                TextButton(onClick = it) {
                    Text("Expire token")
                }
            }
        }
    }
}