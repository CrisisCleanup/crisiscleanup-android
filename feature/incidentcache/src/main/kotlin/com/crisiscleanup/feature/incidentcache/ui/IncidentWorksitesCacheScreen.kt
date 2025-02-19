package com.crisiscleanup.feature.incidentcache.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
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
    val isParametersEnabled = syncParameters != null

    var isMapScrolling by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(
                rememberScrollState(),
                enabled = !isMapScrolling,
            ),
    ) {
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

        CrisisCleanupRadioButton(
            listItemModifier,
            syncParameters?.isAutoCache ?: false,
            text = t("~~Auto download Cases"),
            enabled = isParametersEnabled,
            onSelect = viewModel::resumeCachingCases,
        ) {
            // TODO Downloaded newest Cases action
        }
        CrisisCleanupRadioButton(
            listItemModifier,
            syncParameters?.isPaused ?: false,
            text = t("~~Pause downloading Cases"),
            enabled = isParametersEnabled,
            onSelect = viewModel::pauseCachingCases,
        ) {
            t("~~Resume downloading Cases by selecting to auto download or download Cases in a region")
        }
        CrisisCleanupRadioButton(
            listItemModifier,
            syncParameters?.isRegionBounded ?: false,
            text = t("~~Downloading Cases within specified region"),
            enabled = isParametersEnabled,
            onSelect = viewModel::boundCachingCases,
        ) {
            // TODO Map
            //      Radius
            //      My location button
            //      Download around my location toggle
            //      Refresh/download button
            //      Show if current bounded download is different from settings
        }

        if (isNotProduction) {
            CrisisCleanupTextButton(
                text = "Reset Incident Cases cache",
                onClick = viewModel::resetCaching,
            )
        }
    }
}
