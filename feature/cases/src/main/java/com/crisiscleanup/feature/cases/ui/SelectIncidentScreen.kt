package com.crisiscleanup.feature.cases.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.feature.cases.CasesViewModel
import com.crisiscleanup.feature.cases.IncidentsData
import kotlinx.coroutines.launch

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun SelectIncidentRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    casesViewModel: CasesViewModel = hiltViewModel(),
) {
    val incidentsData by casesViewModel.incidentsData.collectAsStateWithLifecycle()

    val dialogContent: @Composable () -> Unit
    if (incidentsData is IncidentsData.Incidents) {

        var isIncidentSelected by rememberSaveable { mutableStateOf(false) }
        val cs = rememberCoroutineScope()
        val onSelectIncident = remember(casesViewModel) {
            { incident: Incident ->
                isIncidentSelected = true
                cs.launch {
                    casesViewModel.selectIncident(incident)
                    onBackClick()
                }
            }
        }
        val incidents = (incidentsData as IncidentsData.Incidents).incidents
        dialogContent = {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text("Show ${incidents.size} incidents to select")
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onBackClick,
                    enabled = !isIncidentSelected,
                ) {
                    Text("Close")
                }
            }
        }
    } else {
        dialogContent = {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text("No incidents to select")
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onBackClick,
                    enabled = true,
                ) {
                    Text("Close")
                }
            }
        }
    }

    Dialog(onDismissRequest = onBackClick) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            dialogContent()
        }
    }
}