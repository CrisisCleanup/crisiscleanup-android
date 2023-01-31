package com.crisiscleanup.feature.cases.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
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
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
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
) {
    val incidentsData by casesViewModel.incidentsData.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onBackClick) {
        Surface(
            // TODO Wrap height in case content is smaller
            modifier = modifier
                .fillMaxHeight(0.8f),
            // TODO Use style value for round
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier) {
                Text(
                    // TODO Use style for padding
                    modifier = modifier.padding(16.dp),
                    text = stringResource(R.string.change_incident),
                    style = MaterialTheme.typography.headlineMedium,
                )

                var enableInput by rememberSaveable { mutableStateOf(true) }

                when (incidentsData) {
                    IncidentsData.Loading -> {
                        Box(modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }

                    is IncidentsData.Incidents -> {
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
                        val incidents = (incidentsData as IncidentsData.Incidents).incidents
                        val selectedIncidentId by casesViewModel.incidentSelector.incidentId.collectAsStateWithLifecycle()
                        IncidentSelectContent(
                            modifier,
                            selectedIncidentId,
                            incidents,
                            onSelectIncident,
                        )
                    }

                    else -> {
                        Text(
                            // TODO Use constant for padding
                            modifier = modifier.padding(16.dp),
                            text = stringResource(R.string.no_incidents_to_select)
                        )
                    }
                }

                Spacer(modifier.weight(1f))
                CrisisCleanupButton(
                    modifier = modifier
                        .padding(padding)
                        .align(Alignment.End),
                    onClick = onBackClick,
                    enabled = enableInput,
                    textResId = R.string.close
                )
            }
        }
    }
}

@Composable
private fun IncidentSelectContent(
    modifier: Modifier = Modifier,
    selectedIncidentId: Long = -1,
    incidents: List<Incident>,
    onIncidentSelect: (Incident) -> Unit = {},
    spacing: Dp = 16.dp,
) {
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
                        // TODO Wrap width so is automatic
                        .sizeIn(minWidth = 200.dp)
                        .fillParentMaxWidth()
                        // TODO Use style for item padding
                        .clickable {
                            onIncidentSelect(incident)
                        }
                        .padding(spacing),
                    text = incident.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = fontWeight,
                )
            }
        }

        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
        }
    }
}
