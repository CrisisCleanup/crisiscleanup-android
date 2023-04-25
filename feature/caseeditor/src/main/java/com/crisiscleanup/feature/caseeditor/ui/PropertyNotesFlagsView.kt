package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.feature.caseeditor.CaseNotesFlagsDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun PropertyNotesFlagsView(
    viewModel: EditCaseBaseViewModel,
    editor: CaseNotesFlagsDataEditor,
    collapsedNotesVisibleCount: Int = 3,
) {
    var isCreatingNote by remember { mutableStateOf(false) }

    val inputData = editor.notesFlagsInputData

    HighPriorityFlagInput(viewModel, inputData)

    if (inputData.isNewWorksite) {
        MemberOfMyOrgFlagInput(viewModel, inputData)
    }

    val notes by inputData.notesStream.collectAsStateWithLifecycle(emptyList())

    var showAllNotesDialog by remember { mutableStateOf(false) }
    val showAllNotes = remember(inputData) { { showAllNotesDialog = true } }
    val isExpandable = notes.size > collapsedNotesVisibleCount
    Row(
        modifier = if (isExpandable) Modifier
            .fillMaxWidth()
            .listItemHeight()
            .listItemHorizontalPadding()
        else listItemModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = viewModel.translate("formLabels.notes"),
            Modifier.weight(1f),
        )
        if (isExpandable) {
            CrisisCleanupTextButton(
                onClick = showAllNotes,
                text = viewModel.translate("actions.all_notes"),
            )
        }
    }

    val noteModifier = Modifier
        .listItemPadding()
        .listItemNestedPadding()
    for (i in 0 until Integer.min(notes.size, collapsedNotesVisibleCount)) {
        val note = notes[i]
        NoteView(note, noteModifier)
    }

    Row(
        Modifier
            .clickable { isCreatingNote = true }
            .fillMaxWidth()
            .listItemHeight()
            .listItemPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(viewModel.translate("caseView.add_note"))
    }

    if (isCreatingNote) {
        val dismissNoteDialog = { isCreatingNote = false }
        OnCreateNote(viewModel, editor, dismissNoteDialog)
    }

    if (showAllNotesDialog) {
        val dismissDialog = { showAllNotesDialog = false }
        AllNotes(
            notes,
            dismissDialog,
        )
    }
}

@Composable
private fun AllNotes(
    notes: List<WorksiteNote>,
    onDismiss: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            // TODO Common dimensions
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                Box(
                    Modifier
                        // TODO Common dimensions
                        .padding(top = 16.dp)
                        .weight(weight = 1f, fill = false)
                ) {
                    LazyColumn {
                        staticNoteItems(
                            notes,
                            notes.size,
                            listItemModifier,
                        )
                    }
                }
                Box(
                    Modifier
                        .listItemPadding()
                        .align(Alignment.End)
                ) {
                    CrisisCleanupTextButton(
                        onClick = onDismiss,
                        enabled = true,
                        textResId = R.string.close,
                    )
                }
            }
        }
    }
}