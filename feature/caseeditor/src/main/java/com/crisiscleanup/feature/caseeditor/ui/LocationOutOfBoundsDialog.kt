package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.feature.caseeditor.CaseLocationDataEditor

@Composable
internal fun LocationOutOfBoundsDialog(
    editor: CaseLocationDataEditor,
) {
    val translator = LocalAppTranslator.current
    val outOfBoundsData by editor.locationOutOfBounds.collectAsStateWithLifecycle()
    outOfBoundsData?.let {
        val title: String
        val message: String
        val startButton: @Composable () -> Unit
        val endButton: @Composable () -> Unit
        if (it.recentIncident == null) {
            title = translator("Case Outside Current Incident")
            val outsideMessage = translator("caseForm.warning_case_outside_incident")
            message = outsideMessage.replace("{incident}", it.incident.name)
            startButton = {
                CrisisCleanupTextButton(
                    onClick = { editor.cancelOutOfBounds() },
                    text = translator("actions.retry"),
                )
            }
            endButton = {
                CrisisCleanupTextButton(
                    onClick = { editor.acceptOutOfBounds(it) },
                    text = translator("actions.continue_anyway"),
                )
            }
        } else {
            title = translator("Incorrect Location")
            val insideMessage = translator("caseForm.suggested_incident")
            message = insideMessage.replace("{incident}", it.recentIncident.name)
            startButton = {
                CrisisCleanupTextButton(
                    onClick = { editor.changeIncidentOutOfBounds(it) },
                    text = translator("caseForm.yes"),
                )
            }
            endButton = {
                CrisisCleanupTextButton(
                    onClick = { editor.acceptOutOfBounds(it) },
                    text = translator("caseForm.no"),
                )
            }
        }
        CrisisCleanupAlertDialog(
            title = title,
            text = message,
            onDismissRequest = {
                // TODO Visually indicate dialog is modal and an action must be selected
            },
            dismissButton = endButton,
            confirmButton = startButton,
        )
    }
}