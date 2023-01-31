package com.crisiscleanup.feature.cases.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.IncidentsData
import com.crisiscleanup.feature.cases.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun SelectIncidentRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    casesViewModel: CasesViewModel = hiltViewModel(),
    padding: Dp = 16.dp,
    textPadding: Dp = 16.dp,
    cornerRound: Dp = 4.dp,
) {
    val incidentsData by casesViewModel.incidentsData.collectAsStateWithLifecycle()

    // TODO Sometimes on first open the (entire) dialog does not expand fully.
    //      Could be due to incidentsData changing state?

    Dialog(
        onDismissRequest = onBackClick,
        content = {
            Surface(
                modifier = modifier.heightIn(max = 480.dp),
                shape = RoundedCornerShape(cornerRound),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier) {
                    when (incidentsData) {
                        is IncidentsData.Incidents -> {
                            Text(
                                modifier = modifier.padding(textPadding),
                                text = stringResource(R.string.change_incident),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    when (incidentsData) {
                        IncidentsData.Loading -> {
                            Box(
                                modifier
                                    .padding(padding)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is IncidentsData.Incidents -> {
                            val incidents = (incidentsData as IncidentsData.Incidents).incidents
                            IncidentSelectContent(
                                casesViewModel,
                                incidents,
                                modifier,
                                onBackClick,
                                padding,
                            )

                        }

                        else -> NoIncidentsContent(
                            modifier,
                            onBackClick,
                            padding,
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun ColumnScope.IncidentSelectContent(
    casesViewModel: CasesViewModel,
    incidents: List<Incident>,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    padding: Dp = 16.dp,
) {
    var enableInput by rememberSaveable { mutableStateOf(true) }
    val cs = rememberCoroutineScope()
    val onSelectIncident = remember(casesViewModel) {
        { incident: Incident ->
            if (enableInput) {
                enableInput = false
                cs.launch {
                    casesViewModel.selectIncident(incident)
                    onBackClick()
                }
            }
        }
    }
    val selectedIncidentId by casesViewModel.incidentSelector.incidentId.collectAsStateWithLifecycle()

    Box(Modifier.weight(weight = 1f, fill = false)) {
        LazyColumn(
            modifier = modifier
                .testTag("cases:incidents"),
        ) {
            incidents.forEach { incident ->
                val id = incident.id
                val isSelected = id == selectedIncidentId
                val fontWeight = if (isSelected) FontWeight.Bold else null
                item(key = id) {
                    Text(
                        modifier = modifier
                            .fillParentMaxWidth()
                            .clickable {
                                onSelectIncident(incident)
                            }
                            .padding(padding),
                        text = incident.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = fontWeight,
                    )
                }
            }
        }
    }
    Box(modifier.align(Alignment.End)) {
        CrisisCleanupTextButton(
            modifier = modifier
                .padding(padding),
            onClick = onBackClick,
            textResId = R.string.close,
        )
    }
}

@Composable
private fun ColumnScope.NoIncidentsContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    padding: Dp = 16.dp,
    textPadding: Dp = 16.dp,
) {
    Text(
        modifier = modifier.padding(textPadding),
        text = stringResource(R.string.no_incidents_to_select),
        style = MaterialTheme.typography.titleLarge,
    )
    CrisisCleanupTextButton(
        modifier = modifier
            .padding(padding)
            .align(Alignment.End),
        onClick = onBackClick,
        textResId = R.string.close,
    )
}
