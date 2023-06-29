package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.commonassets.DisasterIcon
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.primaryRedColor
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun CaseIncidentView(
    modifier: Modifier = Modifier,
    incident: Incident = EmptyIncident,
    isPendingSync: Boolean = false,
    isSyncing: Boolean = false,
    scheduleSync: () -> Unit = {},
) {
    val incidentName = incident.shortName
    val disasterResId = getDisasterIcon(incident.disaster)
    Row(
        modifier = modifier
            .listItemPadding()
            // Match icon frame size below
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        // TODO Common dimensions
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DisasterIcon(disasterResId, incidentName)
        Text(
            incidentName,
            Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
        )

        if (isSyncing) {
            Box(
                // minimumInteractiveComponentSize > IconButtonTokens.StateLayerSize
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.CloudSync,
                    contentDescription = stringResource(R.string.is_syncing),
                )
            }
        } else if (isPendingSync) {
            CrisisCleanupIconButton(
                onClick = scheduleSync,
                imageVector = CrisisCleanupIcons.CloudOff,
                contentDescription = stringResource(R.string.is_pending_sync),
                tint = primaryRedColor,
            )
        }
    }
}

@Preview("syncing")
@Composable
private fun CaseIncidentSyncingPreview() {
    CrisisCleanupTheme {
        Surface {
            CaseIncidentView(
                incident = EmptyIncident.copy(
                    name = "Big sweeping hurricane across the gulf",
                    shortName = "Big hurricane"
                ),
                isSyncing = true,
            )
        }
    }
}

@Preview("pending-sync")
@Composable
private fun CaseIncidentPendingSyncPreview() {
    CrisisCleanupTheme {
        Surface {
            CaseIncidentView(
                incident = EmptyIncident.copy(
                    name = "Big sweeping hurricane across the gulf",
                    shortName = "Big hurricane"
                ),
                isPendingSync = true,
            )
        }
    }
}
