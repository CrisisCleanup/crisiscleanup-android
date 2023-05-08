package com.crisiscleanup.feature.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton

@Composable
internal fun MenuRoute(
    openSyncLogs: () -> Unit = {},
) {
    MenuScreen(
        openSyncLogs = openSyncLogs,
    )
}

@Composable
internal fun MenuScreen(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel(),
    openSyncLogs: () -> Unit = {},
) {
    val databaseText = viewModel.databaseVersionText

    val expireTokenAction = if (viewModel.isDebuggable) {
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
                CrisisCleanupTextButton(
                    onClick = it,
                    text = "Expire token",
                )
            }

            if (viewModel.isNotProduction) {
                CrisisCleanupTextButton(
                    onClick = openSyncLogs,
                    text = "See sync logs",
                )
            }
        }
    }
}