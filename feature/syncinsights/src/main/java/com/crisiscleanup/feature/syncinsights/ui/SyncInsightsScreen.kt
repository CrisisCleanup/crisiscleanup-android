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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.model.data.SyncLog
import com.crisiscleanup.feature.syncinsights.SyncInsightsViewModel

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

        val pagingLogs = viewModel.syncLogs.collectAsLazyPagingItems()
        val listState = rememberLazyListState()

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
                pagingLogs.itemCount,
                key = pagingLogs.itemKey { it.id },
                contentType = {
                    if (pagingLogs.isContinuingLogType(it)) {
                        "detail-log-item"
                    } else {
                        "one-line-log-item"
                    }
                },
            ) { index ->
                val log = pagingLogs[index]
                if (log == null) {
                    Text(
                        "$index",
                        Modifier.listItemPadding(),
                    )
                } else {
                    val isContinuingLogType = pagingLogs.isContinuingLogType(index)
                    val modifier = if (isContinuingLogType) {
                        Modifier
                            .padding(start = 16.dp)
                            .listItemBottomPadding()
                    } else {
                        Modifier
                            .listItemPadding()
                            .listItemTopPadding()
                    }
                    Column(modifier.clickable { viewModel.onExpandLog(log) }) {
                        SyncLogDetail(log, isContinuingLogType)
                    }
                }
            }

            if (pagingLogs.loadState.append is LoadState.Loading) {
                item(
                    contentType = { "loading" },
                ) {
                    // TODO Loading indicator
                }
            }
        }
    }
}

private fun LazyPagingItems<SyncLog>.isContinuingLogType(index: Int): Boolean {
    return index in 1..<itemCount && get(index - 1)!!.logType == get(index)!!.logType
}

@Composable
private fun SyncLogDetail(log: SyncLog, isContinuingLogType: Boolean) = with(log) {
    if (!isContinuingLogType) {
        val logTypeText = "$logType ${log.logTime.relativeTime}".trim()
        Text(
            logTypeText,
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
