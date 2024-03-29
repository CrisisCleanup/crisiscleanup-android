package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemNestedPadding
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.survivorNoteColor
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.core.model.data.hasSurvivorNote
import com.crisiscleanup.feature.caseeditor.CaseNotesFlagsDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel

@Composable
internal fun PropertyNotesFlagsView(
    viewModel: EditCaseBaseViewModel,
    editor: CaseNotesFlagsDataEditor,
    collapsedNotesVisibleCount: Int = 3,
) {
    val translator = LocalAppTranslator.current
    val isEditable = LocalCaseEditor.current.isEditable

    val inputData = editor.notesFlagsInputData

    HighPriorityFlagInput(inputData, isEditable)

    if (inputData.isNewWorksite) {
        MemberOfMyOrgFlagInput(inputData, isEditable)
    }

    val notes by inputData.notesStream.collectAsStateWithLifecycle(emptyList())
    val otherNotes by inputData.otherNotes.collectAsStateWithLifecycle(emptyList())

    var showAllNotesDialog by remember { mutableStateOf(false) }
    val showAllNotes = remember(inputData) { { showAllNotesDialog = true } }
    val isExpandable = notes.size > collapsedNotesVisibleCount
    Row(
        modifier = if (isExpandable) {
            Modifier
                .fillMaxWidth()
                .listItemHeight()
                .listItemHorizontalPadding()
        } else {
            listItemModifier
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = translator("formLabels.notes"),
            Modifier
                .testTag("propertyLabelNotesText")
                .weight(1f),
        )
        if (isExpandable) {
            CrisisCleanupTextButton(
                onClick = showAllNotes,
                text = translator("actions.all_notes"),
                modifier = Modifier.testTag("propertyAllNotesBtn"),
            )
        }
    }

    if (notes.hasSurvivorNote) {
        SurvivorNoteLegend(listItemModifier)
    }

    val noteModifier = Modifier
        .fillMaxWidth()
        .listItemPadding()
        .listItemNestedPadding()
    val survivorNoteModifier = Modifier
        .background(survivorNoteColor)
        .then(noteModifier)
    for (i in 0 until notes.size.coerceAtMost(collapsedNotesVisibleCount)) {
        val note = notes[i]
        val modifier = if (note.isSurvivor) survivorNoteModifier else noteModifier
        NoteView(note, modifier)
    }

    val saveNote = remember(viewModel) {
        { note: WorksiteNote -> editor.notesFlagsInputData.notes.add(0, note) }
    }
    CrisisCleanupTextArea(
        text = inputData.editingNote,
        onTextChange = { inputData.editingNote = it },
        modifier = listItemModifier,
        label = { Text(translator("caseView.note")) },
        enabled = isEditable,
        imeAction = ImeAction.Default,
    )
    CrisisCleanupButton(
        onClick = {
            val note = WorksiteNote.create().copy(
                note = inputData.editingNote,
            )
            saveNote(note)
            inputData.editingNote = ""
            // TODO Scroll to newly added note?
        },
        modifier = listItemModifier,
        text = translator("actions.add"),
        enabled = isEditable && inputData.editingNote.isNotBlank(),
    )

    if (showAllNotesDialog) {
        val dismissDialog = { showAllNotesDialog = false }
        AllNotes(
            notes,
            otherNotes,
            translator("actions.close"),
            dismissDialog,
        )
    }
}

@Composable
private fun AllNotes(
    notes: List<WorksiteNote>,
    otherNotes: List<Pair<String, String>>,
    dismissText: String = "",
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
                        .weight(weight = 1f, fill = false),
                ) {
                    LazyColumn {
                        otherNoteItems(otherNotes)
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
                        .align(Alignment.End),
                ) {
                    CrisisCleanupTextButton(
                        onClick = onDismiss,
                        enabled = true,
                        text = dismissText,
                    )
                }
            }
        }
    }
}
