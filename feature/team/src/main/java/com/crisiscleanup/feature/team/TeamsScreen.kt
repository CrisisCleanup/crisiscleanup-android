package com.crisiscleanup.feature.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appcomponent.ui.AppTopBar
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.selectincident.SelectIncidentDialog

@Composable
internal fun TeamsRoute(
    openAuthentication: () -> Unit = {},
) {
    TeamsScreen(
        openAuthentication = openAuthentication,
    )
}

@Composable
private fun TeamsScreen(
    viewModel: TeamViewModel = hiltViewModel(),
    openAuthentication: () -> Unit = {},
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val incidentsData by viewModel.incidentsData.collectAsStateWithLifecycle()

    var showIncidentPicker by remember { mutableStateOf(false) }
    val openIncidentsSelect = remember(viewModel) {
        { showIncidentPicker = true }
    }

    val t = LocalAppTranslator.current
    Box {
        Column {
            AppTopBar(
                modifier = Modifier,
                dataProvider = viewModel.appTopBarDataProvider,
                openAuthentication = openAuthentication,
                onOpenIncidents = openIncidentsSelect,
            )

            Text(
                text = t("~~My Teams"),
                modifier = listItemModifier,
                style = LocalFontStyles.current.header1,
            )

            // TODO List teams

            // TODO Create team action

            // TODO All (other) teams
        }
        BusyIndicatorFloatingTopCenter(isLoading)
    }

    if (showIncidentPicker) {
        val closeDialog = { showIncidentPicker = false }
        val selectedIncidentId by viewModel.incidentSelector.incidentId.collectAsStateWithLifecycle()
        val setSelected = remember(viewModel) {
            { incident: Incident ->
                viewModel.loadSelectIncidents.selectIncident(incident)
            }
        }
        SelectIncidentDialog(
            rememberKey = viewModel,
            onBackClick = closeDialog,
            incidentsData = incidentsData,
            selectedIncidentId = selectedIncidentId,
            onSelectIncident = setSelected,
            onRefreshIncidents = viewModel::refreshIncidentsAsync,
        )
    }
}

@Composable
internal fun TeamView(
    team: CleanupTeam,
) {
}
