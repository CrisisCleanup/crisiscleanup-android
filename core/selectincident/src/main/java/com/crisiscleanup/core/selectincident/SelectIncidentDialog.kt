package com.crisiscleanup.core.selectincident

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.launch

@Composable
private fun WrapInDialog(
    onBackClick: () -> Unit,
    // TODO Common dimensions
    cornerRound: Dp = 8.dp,
    content: @Composable () -> Unit,
) = Dialog(
    onDismissRequest = onBackClick,
    properties = DialogProperties(usePlatformDefaultWidth = false),
) {
    Surface(
        // TODO Adjust when screen is wide
        Modifier.fillMaxWidth(0.8f),
        shape = RoundedCornerShape(cornerRound),
        color = MaterialTheme.colorScheme.surface,
    ) {
        content()
    }
}

@Composable
fun SelectIncidentDialog(
    rememberKey: Any,
    onBackClick: () -> Unit,
    incidentsData: IncidentsData,
    selectedIncidentId: Long,
    onSelectIncident: (Incident) -> Unit,
    onRefreshIncidentsAsync: suspend () -> Unit = {},
    onRefreshIncidents: () -> Unit = {},
    isLoadingIncidents: Boolean = false,
    padding: Dp = 16.dp,
    textPadding: Dp = 16.dp,
) {
    val t = LocalAppTranslator.current

    WrapInDialog(onBackClick) {
        when (incidentsData) {
            IncidentsData.Loading -> {
                Box(Modifier.padding(padding)) {
                    CircularProgressIndicator()
                }
            }

            is IncidentsData.Incidents -> {
                Column {
                    Text(
                        modifier = Modifier
                            .testTag("selectIncidentHeader")
                            .padding(textPadding),
                        text = t("nav.change_incident"),
                        style = LocalFontStyles.current.header3,
                    )

                    val incidents = incidentsData.incidents
                    IncidentSelectContent(
                        rememberKey = rememberKey,
                        selectedIncidentId = selectedIncidentId,
                        incidents = incidents,
                        onSelectIncident = onSelectIncident,
                        onBackClick = onBackClick,
                        onRefreshIncidents = onRefreshIncidentsAsync,
                        padding = padding,
                    )
                }
            }

            else -> {
                RefreshIncidentsView(
                    isLoadingIncidents,
                    onRefreshIncidents,
                    // TODO Common dimensions
                    Modifier.sizeIn(maxWidth = 300.dp)
                        .then(listItemModifier),
                    padding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.IncidentSelectContent(
    rememberKey: Any,
    selectedIncidentId: Long,
    incidents: List<Incident>,
    onSelectIncident: (Incident) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onRefreshIncidents: suspend () -> Unit = {},
    padding: Dp = 16.dp,
) {
    var enableInput by rememberSaveable { mutableStateOf(true) }
    val rememberOnSelectIncident = remember(rememberKey) {
        { incident: Incident ->
            if (enableInput) {
                enableInput = false
                onSelectIncident(incident)
                onBackClick()
            }
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshingIncidents by remember { mutableStateOf(false) }
    val refreshIncidents = remember(onRefreshIncidents, listState) {
        {
            coroutineScope.launch {
                isRefreshingIncidents = true
                try {
                    onRefreshIncidents()
                    listState.animateScrollToItem(0)
                } finally {
                    isRefreshingIncidents = false
                }
            }
            Unit
        }
    }
    PullToRefreshBox(
        modifier = Modifier
            .weight(weight = 1f, fill = false),
        isRefreshing = isRefreshingIncidents,
        onRefresh = refreshIncidents,
    ) {
        LazyColumn(
            state = listState,
            modifier = modifier,
        ) {
            items(
                incidents.size,
                key = { incidents[it].id },
            ) {
                val incident = incidents[it]
                val id = incident.id
                val isSelected = id == selectedIncidentId
                val fontWeight = if (isSelected) FontWeight.Bold else null
                Text(
                    modifier = modifier
                        .testTag("selectIncidentItem_$id")
                        .fillParentMaxWidth()
                        .clickable(enabled = enableInput) {
                            rememberOnSelectIncident(incident)
                        }
                        .padding(padding),
                    text = incident.displayLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = fontWeight,
                )
            }
        }
        LaunchedEffect(Unit) {
            val selectedIndex = incidents.indexOfFirst { it.id == selectedIncidentId }
            if (selectedIndex > 0) {
                listState.animateScrollToItem(selectedIndex)
            }
        }
    }
    Box(modifier.align(Alignment.End)) {
        CrisisCleanupTextButton(
            modifier = modifier
                .padding(padding),
            onClick = onBackClick,
            enabled = enableInput,
            text = LocalAppTranslator.current("actions.close"),
        )
    }
}

@Composable
fun RefreshIncidentsView(
    isLoadingIncidents: Boolean,
    onRefreshIncidents: () -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
) {
    val t = LocalAppTranslator.current

    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(padding, Alignment.CenterVertically),
    ) {
        Text(
            t("info.no_incidents_to_select"),
            style = LocalFontStyles.current.header3,
        )

        Text(t("info.incident_load_error"))

        CrisisCleanupTextButton(
            Modifier.align(Alignment.End),
            enabled = !isLoadingIncidents,
            text = t("actions.retry"),
            onClick = onRefreshIncidents,
        )
    }
}
