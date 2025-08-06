package com.crisiscleanup.core.commoncase.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor

@Composable
fun SyncStatusView(
    isSyncing: Boolean,
    isPendingSync: Boolean,
    scheduleSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalAppTranslator.current
    if (isSyncing) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CrisisCleanupIcons.CloudSync,
                contentDescription = t("info.is_syncing"),
                modifier = Modifier.testTag("isSyncingIcon"),
            )
        }
    } else if (isPendingSync) {
        CrisisCleanupIconButton(
            onClick = scheduleSync,
            imageVector = CrisisCleanupIcons.CloudOff,
            contentDescription = t("info.is_pending_sync"),
            tint = primaryOrangeColor,
            modifier = Modifier.testTag("isPendingSyncAction"),
        )
    }
}
