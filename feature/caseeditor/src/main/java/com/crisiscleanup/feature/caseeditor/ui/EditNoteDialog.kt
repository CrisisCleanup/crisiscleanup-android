package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.textBoxHeight
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
    AlertDialog(
        title = { Text(dialogTitle) },
        text = {
            val focusRequester = FocusRequester()

            val keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
            )
            val keyboardActions = KeyboardActions(
                onDone = { saveNote() },
            )
            OutlinedTextField(
                noteContent,
                { value: String -> noteContent = value },
                modifier = Modifier
                    .textBoxHeight()
                    .focusRequester(focusRequester),
                label = { Text(translator("caseView.note")) },
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
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
