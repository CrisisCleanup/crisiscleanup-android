package com.crisiscleanup.core.commoncase.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.commonassets.DisasterIcon
import com.crisiscleanup.core.commonassets.R
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor

@Composable
fun IncidentHeaderView(
    modifier: Modifier = Modifier,
    incidentName: String = "",
    @DrawableRes disasterResId: Int,
    isPendingSync: Boolean = false,
    isSyncing: Boolean = false,
    scheduleSync: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .listItemPadding()
            .listItemHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedBy,
    ) {
        DisasterIcon(disasterResId, incidentName)
        Text(
            incidentName,
            Modifier
                .testTag("caseViewIncidentName")
                .weight(1f),
            style = LocalFontStyles.current.header1,
        )

        val t = LocalAppTranslator.current
        if (isSyncing) {
            Box(
                // minimumInteractiveComponentSize > IconButtonTokens.StateLayerSize
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.CloudSync,
                    contentDescription = t("info.is_syncing"),
                    modifier = Modifier.testTag("caseViewIsSyncingIcon"),
                )
            }
        } else if (isPendingSync) {
            CrisisCleanupIconButton(
                onClick = scheduleSync,
                imageVector = CrisisCleanupIcons.Cloud,
                contentDescription = t("info.is_pending_sync"),
                tint = primaryOrangeColor,
                modifier = Modifier.testTag("caseViewIsPendingSyncIconBtn"),
            )
        }
    }
}

@Preview("syncing")
@Composable
private fun IncidentHeaderSyncingPreview() {
    CrisisCleanupTheme {
        Surface {
            IncidentHeaderView(
                incidentName = "Big hurricane",
                disasterResId = R.drawable.ic_flood_thunder,
                isSyncing = true,
            )
        }
    }
}

@Preview("pending-sync")
@Composable
private fun IncidentHeaderPendingSyncPreview() {
    CrisisCleanupTheme {
        Surface {
            IncidentHeaderView(
                incidentName = "Big hurricane",
                disasterResId = R.drawable.ic_flood_thunder,
                isPendingSync = true,
            )
        }
    }
}
