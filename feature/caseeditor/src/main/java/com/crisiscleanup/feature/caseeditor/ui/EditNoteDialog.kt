package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
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
    AlertDialog(
        title = { Text(dialogTitle) },
        text = {
            val focusRequester = FocusRequester()
            OutlinedTextField(
                modifier = Modifier
                    .heightIn(min = 128.dp, max = 256.dp)
                    .focusRequester(focusRequester),
                value = noteContent,
                onValueChange = { value: String -> noteContent = value },
                label = { Text(stringResource(R.string.note)) },
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
                onClick = {
                    val saveNote = note.copy(note = noteContent.trim())
                    isSaving = true
                    onSave(saveNote)
                },
                enabled = !isSaving,
            )
        },
    )
}
