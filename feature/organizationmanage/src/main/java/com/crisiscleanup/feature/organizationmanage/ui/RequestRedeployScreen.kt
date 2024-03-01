package com.crisiscleanup.feature.organizationmanage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.ListOptionsDropdown
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.feature.organizationmanage.RequestRedeployViewModel
import com.crisiscleanup.feature.organizationmanage.RequestRedeployViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestRedeployRoute(
    onBack: () -> Unit = {},
    viewModel: RequestRedeployViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val isLoading = viewState == RequestRedeployViewState.Loading
    val isRedeployRequested = viewModel.isRedeployRequested

    val t = LocalAppTranslator.current
    Column {
        TopAppBarBackAction(
            title = t("requestRedeploy.request_redeploy"),
            onAction = onBack,
        )

        if (isLoading) {
            Box {
                BusyIndicatorFloatingTopCenter(true)
            }
        } else if (isRedeployRequested) {
            Text(
                t("requestRedeploy.request_redeploy_success"),
                listItemModifier,
            )
        } else {
            (viewState as? RequestRedeployViewState.Ready)?.let { readyState ->
                val incidents = readyState.incidents
                val isTransient by viewModel.isTransient.collectAsStateWithLifecycle(true)
                val isEditable = !isTransient
                val errorMessage = viewModel.redeployErrorMessage
                val requestedIncidentIds = viewModel.requestedIncidentIds
                val isRequestingRedeploy by viewModel.isRequestingRedeploy.collectAsStateWithLifecycle()
                var selectedIncident by remember { mutableStateOf(EmptyIncident) }
                val isIncidentEditable = remember(requestedIncidentIds) {
                    { incident: Incident ->
                        !requestedIncidentIds.contains(incident.id)
                    }
                }

                Text(
                    t("requestRedeploy.choose_an_incident"),
                    listItemModifier,
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        errorMessage,
                        listItemModifier,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                val selectIncidentHint = t("actions.select_incident")
                var showDropdown by remember { mutableStateOf(false) }
                val onSelectIncident = remember(viewModel) {
                    { incident: Incident ->
                        selectedIncident = incident
                        showDropdown = false
                    }
                }
                val onHideDropdown = remember(viewModel) { { showDropdown = false } }
                ListOptionsDropdown(
                    text = selectedIncident.name.ifBlank { selectIncidentHint },
                    isEditable = isEditable,
                    onToggleDropdown = { showDropdown = !showDropdown },
                    modifier = Modifier.padding(16.dp),
                    dropdownIconContentDescription = selectIncidentHint,
                ) { contentSize ->
                    IncidentsDropdown(
                        incidents,
                        contentSize,
                        showDropdown,
                        onSelectIncident,
                        onHideDropdown,
                        isEditable = isIncidentEditable,
                    )
                }

                Spacer(Modifier.weight(1f))

                Row(
                    listItemModifier,
                    horizontalArrangement = listItemSpacedBy,
                ) {
                    BusyButton(
                        Modifier
                            .testTag("requestRedeployCancelAction")
                            .weight(1f),
                        text = t("actions.cancel"),
                        enabled = isEditable,
                        indicateBusy = false,
                        onClick = onBack,
                    )
                    BusyButton(
                        Modifier
                            .testTag("requestRedeploySubmitAction")
                            .weight(1f),
                        text = t("actions.submit"),
                        enabled = isEditable && selectedIncident != EmptyIncident,
                        indicateBusy = isRequestingRedeploy,
                        onClick = { viewModel.requestRedeploy(selectedIncident) },
                    )
                }
            }
        }
    }
}
