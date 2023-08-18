package com.crisiscleanup.feature.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val translator = LocalAppTranslator.current

    val isSharingAnalytics by viewModel.isSharingAnalytics.collectAsStateWithLifecycle(false)
    val shareAnalytics = remember(viewModel) {
        { b: Boolean ->
            viewModel.shareAnalytics(b)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                modifier = listItemModifier,
                text = viewModel.versionText,
            )

            CrisisCleanupButton(
                modifier = Modifier.listItemPadding(),
                text = translator("info.give_app_feedback"),
                onClick = openUserFeedback,
            )

            Row(
                listItemModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "~~Share analytics",
                    Modifier.weight(1f),
                )
                Switch(
                    checked = isSharingAnalytics,
                    onCheckedChange = shareAnalytics,
                )
            }

            if (viewModel.isDebuggable) {
                MenuScreenDebug()
            }

            if (viewModel.isNotProduction) {
                CrisisCleanupTextButton(
                    onClick = openSyncLogs,
                    text = "See sync logs",
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // TODO Open in WebView?
            val uriHandler = LocalUriHandler.current
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    translator("publicNav.terms"),
                    Modifier
                        .listItemPadding()
                        .clickable { uriHandler.openUri("https://crisiscleanup.org/terms") },
                )
                Text(
                    translator("nav.privacy"),
                    Modifier
                        .listItemPadding()
                        .clickable { uriHandler.openUri("https://crisiscleanup.org/privacy") },
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