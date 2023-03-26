package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.feature.caseeditor.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteDialog(
    note: WorksiteNote,
    dialogTitle: String,
    onSave: (WorksiteNote) -> Unit = {},
    onCancel: () -> Unit = {},
) {
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
                keyboardType = KeyboardType.Password,
            )
            val keyboardActions = KeyboardActions(
                onDone = { saveNote() },
            )
            OutlinedTextField(
                noteContent,
                { value: String -> noteContent = value },
                modifier = Modifier
                    // TODO Use common dimensions
                    .heightIn(min = 128.dp, max = 256.dp)
                    .focusRequester(focusRequester),
                label = { Text(stringResource(R.string.note)) },
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
                textResId = android.R.string.cancel,
                onClick = onCancel
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                textResId = R.string.save,
                onClick = saveNote,
                enabled = !isSaving,
            )
        },
    )
}
