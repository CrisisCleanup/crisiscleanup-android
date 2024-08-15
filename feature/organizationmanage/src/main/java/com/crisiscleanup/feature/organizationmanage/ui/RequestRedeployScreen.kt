package com.crisiscleanup.feature.organizationmanage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.green600
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.EmptyIncidentIdNameType
import com.crisiscleanup.core.model.data.IncidentIdNameType
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
            Box(Modifier.fillMaxSize()) {
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
                if (incidents.isEmpty()) {
                    Text(
                        t("~~There are no Incidents left for deploying."),
                        listItemModifier,
                    )
                } else {
                    val isTransient by viewModel.isTransient.collectAsStateWithLifecycle(true)
                    val isEditable = !isTransient
                    val errorMessage = viewModel.redeployErrorMessage
                    val requestedIncidentIds = readyState.requestedIncidentIds
                    val approvedIncidentIds = readyState.approvedIncidentIds
                    val isRequestingRedeploy by viewModel.isRequestingRedeploy.collectAsStateWithLifecycle()
                    var selectedIncident by remember { mutableStateOf(EmptyIncidentIdNameType) }
                    val onSelectIncident = remember(viewModel) {
                        { incident: IncidentIdNameType ->
                            selectedIncident = incident
                        }
                    }
                    val selectIncidentHint = t("actions.select_incident")

                    RequestRedeployContent(
                        isEditable,
                        incidents,
                        requestedIncidentIds,
                        approvedIncidentIds,
                        errorMessage,
                        selectedIncident.name.ifBlank { selectIncidentHint },
                        selectIncidentHint,
                        onSelectIncident,
                        rememberKey = viewModel,
                    )

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
                            colors = cancelButtonColors(),
                        )
                        BusyButton(
                            Modifier
                                .testTag("requestRedeploySubmitAction")
                                .weight(1f),
                            text = t("actions.submit"),
                            enabled = isEditable && selectedIncident != EmptyIncidentIdNameType,
                            indicateBusy = isRequestingRedeploy,
                            onClick = { viewModel.requestRedeploy(selectedIncident) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRedeployContent(
    isEditable: Boolean,
    incidents: List<IncidentIdNameType>,
    requestedIncidentIds: Set<Long>,
    approvedIncidentIds: Set<Long>,
    errorMessage: String,
    selectedIncidentText: String,
    selectIncidentHint: String,
    setSelectedIncident: (IncidentIdNameType) -> Unit,
    rememberKey: Any,
) {
    val t = LocalAppTranslator.current

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

    if (incidents.isNotEmpty()) {
        var showDropdown by remember { mutableStateOf(false) }
        val onSelectIncident = remember(rememberKey) {
            { incident: IncidentIdNameType ->
                setSelectedIncident(incident)
                showDropdown = false
            }
        }
        val onHideDropdown = remember(rememberKey) { { showDropdown = false } }
        ListOptionsDropdown(
            text = selectedIncidentText,
            isEditable = isEditable,
            onToggleDropdown = { showDropdown = !showDropdown },
            modifier = Modifier.padding(16.dp),
            dropdownIconContentDescription = selectIncidentHint,
        ) { contentSize ->
            IncidentsDropdown(
                contentSize,
                showDropdown,
                onHideDropdown,
            ) {
                IncidentOptions(
                    incidents,
                    approvedIncidentIds,
                    requestedIncidentIds,
                    onSelectIncident,
                )
            }
        }
    }
}

@Composable
private fun IncidentOptions(
    incidents: List<IncidentIdNameType>,
    approvedIds: Set<Long>,
    requestedIds: Set<Long>,
    onSelect: (IncidentIdNameType) -> Unit,
) {
    val t = LocalAppTranslator.current
    for (incident in incidents) {
        key(incident.id) {
            val isApproved = approvedIds.contains(incident.id)
            val isRequested = requestedIds.contains(incident.id)
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = listItemSpacedByHalf,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            incident.name,
                            Modifier.weight(1f),
                            style = LocalFontStyles.current.header4,
                        )
                        if (isApproved) {
                            Icon(
                                CrisisCleanupIcons.Check,
                                contentDescription = t("~~{incident_name} is already approved")
                                    .replace("{incident_name}", incident.shortName),
                                tint = green600,
                            )
                        } else if (isRequested) {
                            Icon(
                                CrisisCleanupIcons.PendingRequestRedeploy,
                                contentDescription = t("~~{incident_name} is already awaiting redeploy")
                                    .replace("{incident_name}", incident.shortName),
                            )
                        }
                    }
                },
                onClick = { onSelect(incident) },
                modifier = Modifier.optionItemHeight(),
                enabled = !(isApproved || isRequested),
            )
        }
    }
}
