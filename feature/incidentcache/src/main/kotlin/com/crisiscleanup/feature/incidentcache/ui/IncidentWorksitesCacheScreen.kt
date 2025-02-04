package com.crisiscleanup.feature.incidentcache.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.feature.incidentcache.IncidentWorksitesCacheViewModel

@Composable
fun IncidentWorksitesCacheRoute(
    onBack: () -> Unit,
) {
    IncidentWorksitesCacheScreen(
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidentWorksitesCacheScreen(
    onBack: () -> Unit,
    viewModel: IncidentWorksitesCacheViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    val incident by viewModel.incident.collectAsStateWithLifecycle()
    val isSyncingIncident by viewModel.isSyncing.collectAsStateWithLifecycle()

    val isNotProduction = viewModel.isNotProduction

    val lastSynced by viewModel.lastSynced.collectAsStateWithLifecycle()

    val syncParameters by viewModel.syncingParameters.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBarBackAction(
            title = incident.shortName,
            onAction = onBack,
        )

        val syncedText = lastSynced?.let {
            t("~~Synced {sync_date}")
                .replace("{sync_date}", it)
        } ?: t("~~Awaiting sync of {incident_name}")
            .replace("{incident_name}", incident.shortName)
        Row(
            listItemModifier,
            horizontalArrangement = listItemSpacedBy,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(syncedText)

            // TODO remove if buttons show loading state
            AnimatedBusyIndicator(
                isSyncingIncident,
                padding = 0.dp,
            )
        }

        if (syncParameters.isPaused) {
            // TODO
        } else if (syncParameters.isRegionBounded) {
            // TODO
        } else {
            // TODO
        }

        // TODO Stop and sync actions, enabling appropriately

        if (isNotProduction) {
            CrisisCleanupTextButton(
                text = "Reset caching",
                onClick = viewModel::resetCaching,
            )
        }
    }
}
