package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.commoncase.ui.IncidentHeaderView
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident

@Composable
internal fun CaseIncidentView(
    modifier: Modifier = Modifier,
    incident: Incident = EmptyIncident,
    isPendingSync: Boolean = false,
    isSyncing: Boolean = false,
    scheduleSync: () -> Unit = {},
) {
    IncidentHeaderView(
        modifier,
        incidentName = incident.shortName,
        disasterResId = getDisasterIcon(incident.disaster),
        isPendingSync = isPendingSync,
        isSyncing = isSyncing,
        scheduleSync = scheduleSync,
    )
}

@Preview("syncing")
@Composable
private fun CaseIncidentSyncingPreview() {
    CrisisCleanupTheme {
        Surface {
            CaseIncidentView(
                incident = EmptyIncident.copy(
                    name = "Big sweeping hurricane across the gulf",
                    shortName = "Big hurricane",
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
                    shortName = "Big hurricane",
                ),
                isPendingSync = true,
            )
        }
    }
}
