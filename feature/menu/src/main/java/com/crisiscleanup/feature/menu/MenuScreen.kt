package com.crisiscleanup.feature.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy

@Composable
internal fun MenuRoute(
    openUserFeedback: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    MenuScreen(
        openUserFeedback = openUserFeedback,
        openSyncLogs = openSyncLogs,
    )
}

@Composable
internal fun MenuScreen(
    viewModel: MenuViewModel = hiltViewModel(),
    openUserFeedback: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                modifier = listItemModifier,
                text = viewModel.versionText,
            )

            CrisisCleanupButton(
                modifier = Modifier.listItemPadding(),
                text = LocalAppTranslator.current("info.give_app_feedback"),
                onClick = openUserFeedback,
            )

            if (viewModel.isDebuggable) {
                MenuScreenDebug()
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

@Composable
internal fun MenuScreenDebug(
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val databaseText = viewModel.databaseVersionText
    Text(
        modifier = listItemModifier,
        text = databaseText,
    )

    Row(horizontalArrangement = listItemSpacedBy) {
        CrisisCleanupTextButton(
            onClick = { viewModel.clearRefreshToken() },
            text = "Clear refresh token",
        )

        CrisisCleanupTextButton(
            onClick = { viewModel.simulateTokenExpired() },
            text = "Expire token",
        )
    }

    CrisisCleanupTextButton(
        onClick = { viewModel.syncWorksitesFull() },
        text = "Sync full",
    )
}
