package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel

@Composable
internal fun LocationOutOfBoundsDialog(
    viewModel: EditCaseBaseViewModel,
    editor: CaseLocationDataEditor,
) {
    val outOfBoundsData by editor.locationOutOfBounds.collectAsStateWithLifecycle()
    outOfBoundsData?.let {
        val title: String
        val message: String
        val startButton: @Composable () -> Unit
        val endButton: @Composable () -> Unit
        if (it.recentIncident == null) {
            title = viewModel.translate("Case Outside Current Incident")
            val outsideMessage = viewModel.translate("caseForm.warning_case_outside_incident")
            message = outsideMessage.replace("{incident}", it.incident.name)
            startButton = {
                CrisisCleanupTextButton(
                    onClick = { editor.cancelOutOfBounds() },
                    text = viewModel.translate("actions.retry"),
                )
            }
            endButton = {
                CrisisCleanupTextButton(
                    onClick = { editor.acceptOutOfBounds(it) },
                    text = viewModel.translate("actions.continue_anyway"),
                )
            }
        } else {
            title = viewModel.translate("Incorrect Location")
            val insideMessage = viewModel.translate("caseForm.suggested_incident")
            message = insideMessage.replace("{incident}", it.recentIncident.name)
            startButton = {
                CrisisCleanupTextButton(
                    onClick = { editor.changeIncidentOutOfBounds(it) },
                    text = viewModel.translate("caseForm.yes"),
                )
            }
            endButton = {
                CrisisCleanupTextButton(
                    onClick = { editor.acceptOutOfBounds(it) },
                    text = viewModel.translate("caseForm.no"),
                )
            }
        }
        AlertDialog(
            title = { Text(title) },
            text = { Text(message) },
            onDismissRequest = {
                // TODO Visually indicate dialog is modal and an action must be selected
            },
            dismissButton = endButton,
            confirmButton = startButton,
        )
    }
}