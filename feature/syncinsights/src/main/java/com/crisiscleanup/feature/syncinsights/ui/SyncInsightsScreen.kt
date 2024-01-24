package com.crisiscleanup.feature.syncinsights.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.feature.syncinsights.SyncInsightsViewModel
import com.crisiscleanup.feature.syncinsights.SyncLogItem

@Composable
internal fun SyncInsightsRoute(
    viewModel: SyncInsightsViewModel = hiltViewModel(),
    openCase: (Long, Long) -> Boolean = { _, _ -> false },
) {
    Column {
        val pendingSync by viewModel.worksitesPendingSync.collectAsStateWithLifecycle()
        val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle(false)
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Sync insights",
                Modifier.weight(1f),
                style = LocalFontStyles.current.header5,
            )

            if (pendingSync.isNotEmpty()) {
                CrisisCleanupTextButton(
                    text = "Sync",
                    onClick = { viewModel.syncPending() },
                    enabled = !isSyncing,
                )
            }
        }

        val logs by viewModel.syncLogs.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()

        val listBlockPosition by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex / viewModel.listBlockSize
            }
        }
        viewModel.onListBlockPosition(listBlockPosition)

        val openWorksiteId by viewModel.openWorksiteId
        if (openWorksiteId.second != 0L) {
            openCase(openWorksiteId.first, openWorksiteId.second)
            viewModel.openWorksiteId.value = Pair(0, 0)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            if (pendingSync.isNotEmpty()) {
                item(
                    contentType = { "single-line-header" },
                ) {
                    Text(
                        "Pending",
                        modifier = listItemModifier,
                        style = LocalFontStyles.current.header4,
                    )
                }
                items(
                    pendingSync,
                    key = { it },
                    contentType = { "pending-text" },
                ) {
                    Text(
                        it,
                        modifier = listItemModifier,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item(
                contentType = { "single-line-header" },
            ) {
                Text(
                    "Logs",
                    modifier = listItemModifier,
                    style = LocalFontStyles.current.header4,
                )
            }

            items(
                logs.count,
                key = { it },
                contentType = {
                    if (logs.getLog(it)?.isContinuingLogType == true) {
                        "detail-log-item"
                    } else {
                        "one-line-log-item"
                    }
                },
            ) { index ->
                val log = logs.getLog(index)
                if (log == null) {
                    Text(
                        "$index",
                        Modifier.listItemPadding(),
                    )
                } else {
                    val modifier = if (log.isContinuingLogType) {
                        Modifier
                            .padding(start = 16.dp)
                            .listItemBottomPadding()
                    } else {
                        Modifier
                            .listItemPadding()
                            .listItemTopPadding()
                    }
                    Column(modifier.clickable { viewModel.onExpandLog(log.syncLog) }) {
                        SyncLogDetail(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncLogDetail(log: SyncLogItem) = with(log.syncLog) {
    if (!log.isContinuingLogType) {
        Text(
            "$logType ${log.relativeTime}",
            style = LocalFontStyles.current.header5,
        )
    }
    Text(message)
    if (details.isNotBlank()) {
        Text(
            details,
            Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
