package com.crisiscleanup.feature.cases.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.feature.cases.SelectIncidentViewModel

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
    onBackClick: () -> Unit,
    selectIncidentViewModel: SelectIncidentViewModel = hiltViewModel(),
    padding: Dp = 16.dp,
    textPadding: Dp = 16.dp,
) {
    val incidentsData by selectIncidentViewModel.incidentsData.collectAsStateWithLifecycle(
        IncidentsData.Loading
    )

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
                        modifier = Modifier.padding(textPadding),
                        text = LocalAppTranslator.current("nav.change_incident"),
                        style = LocalFontStyles.current.header3,
                    )

                    val incidents = (incidentsData as IncidentsData.Incidents).incidents
                    IncidentSelectContent(
                        selectIncidentViewModel,
                        incidents,
                        onBackClick = onBackClick,
                        padding = padding,
                    )
                }
            }

            else -> {
                NoIncidentsContent(
                    onBackClick = onBackClick,
                    padding = padding,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.IncidentSelectContent(
    selectIncidentViewModel: SelectIncidentViewModel,
    incidents: List<Incident>,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    padding: Dp = 16.dp,
) {
    var enableInput by rememberSaveable { mutableStateOf(true) }
    val onSelectIncident = remember(selectIncidentViewModel) {
        { incident: Incident ->
            if (enableInput) {
                enableInput = false
                selectIncidentViewModel.selectIncident(incident)
                onBackClick()
            }
        }
    }
    val selectedIncidentId by selectIncidentViewModel.incidentSelector.incidentId.collectAsStateWithLifecycle()

    Box(Modifier.weight(weight = 1f, fill = false)) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = modifier
                .testTag("cases:incidents"),
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
                        .fillParentMaxWidth()
                        .clickable(enabled = enableInput) {
                            onSelectIncident(incident)
                        }
                        .padding(padding),
                    text = incident.name,
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
private fun NoIncidentsContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    padding: Dp = 16.dp,
    textPadding: Dp = 16.dp,
) {
    Column(modifier) {
        Text(
            modifier = modifier.padding(textPadding),
            text = LocalAppTranslator.current("info.no_incidents_to_select"),
            style = LocalFontStyles.current.header3,
        )
        CrisisCleanupTextButton(
            modifier = modifier
                .padding(padding)
                .align(Alignment.End),
            onClick = onBackClick,
            text = LocalAppTranslator.current("actions.close"),
        )
    }
}
