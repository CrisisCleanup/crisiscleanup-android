package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.model.data.WorksiteNote

@Composable
fun EditNoteDialog(
    note: WorksiteNote,
    dialogTitle: String,
    onSave: (WorksiteNote) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current.translator
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var noteContent by rememberSaveable { mutableStateOf(note.note.trim()) }
    val saveNote = {
        isSaving = true
        val noteState = note.copy(note = noteContent.trim())
        onSave(noteState)
    }
    CrisisCleanupAlertDialog(
        title = dialogTitle,
        textContent = {
            CrisisCleanupTextArea(
                text = noteContent,
                onTextChange = { value: String -> noteContent = value },
                label = { Text(translator("caseView.note")) },
                onDone = { saveNote() },
                hasFocus = true,
                enabled = true,
            )
        },
        onDismissRequest = onCancel,
        dismissButton = {
            CrisisCleanupTextButton(
                text = translator("actions.cancel"),
                onClick = onCancel
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                text = translator("actions.add"),
                onClick = saveNote,
                enabled = !isSaving,
            )
        },
    )
}
