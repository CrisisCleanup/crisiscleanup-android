package com.crisiscleanup.feature.incidentcache.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
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

    val isNotProduction = viewModel.isNotProduction

    val lastSynced by viewModel.lastSynced.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBarBackAction(
            title = incident.shortName,
            onAction = onBack,
        )

        val syncedText = lastSynced?.let {
            t("~~Synced {sync_date}")
                .replace("{sync_date}", it)
        } ?: t("~~Awaiting sync")
        Text(syncedText)

        Text("Incident cache")

        if (isNotProduction) {
            CrisisCleanupTextButton(
                text = "Reset caching",
                onClick = viewModel::resetCaching,
            )
        }
    }
}
