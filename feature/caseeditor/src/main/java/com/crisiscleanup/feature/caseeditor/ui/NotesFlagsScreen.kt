package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.feature.caseeditor.model.NotesFlagsInputData
import com.crisiscleanup.feature.caseeditor.model.getRelativeDate

@Composable
internal fun NoteView(
    note: WorksiteNote,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // TODO Different styling when isSurvivor and not
        val relativeDate = note.getRelativeDate()
        if (relativeDate.isNotEmpty()) {
            Text(
                text = relativeDate,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(text = note.note)
    }
}

internal fun LazyListScope.staticNoteItems(
    notes: List<WorksiteNote>,
    visibleCount: Int,
    modifier: Modifier,
) {
    // TODO Animate on expand/collapse
    val count = notes.size.coerceAtMost(visibleCount)
    items(
        count,
        key = {
            val note = notes[it]
            if (note.id > 0) note.id
            else note.createdAt.toEpochMilliseconds()
        },
        contentType = { "item-note" },
    ) {
        val note = notes[it]
        NoteView(note, modifier)
    }
}

@Composable
internal fun OnCreateNote(
    saveNote: (WorksiteNote) -> Unit = {},
    dismissDialog: () -> Unit = {},
) {
    val onSave = { note: WorksiteNote ->
        if (note.note.isNotBlank()) {
            saveNote(note)
        }
        dismissDialog()
    }
    EditNoteDialog(
        note = WorksiteNote.create(),
        dialogTitle = LocalAppTranslator.current.translator("caseView.add_note"),
        onSave = onSave,
        onCancel = dismissDialog,
    )
}

@Composable
internal fun HighPriorityFlagInput(
    inputData: NotesFlagsInputData,
    enabled: Boolean = true,
) {
    val toggleHighPriority = remember(inputData) {
        { inputData.isHighPriority = !inputData.isHighPriority }
    }
    val updateHighPriority =
        remember(inputData) { { b: Boolean -> inputData.isHighPriority = b } }
    CrisisCleanupTextCheckbox(
        listItemModifier.listCheckboxAlignStartOffset(),
        inputData.isHighPriority,
        text = LocalAppTranslator.current.translator("flag.flag_high_priority"),
        onToggle = toggleHighPriority,
        onCheckChange = updateHighPriority,
        enabled = enabled,
    )
}

@Composable
internal fun MemberOfMyOrgFlagInput(
    inputData: NotesFlagsInputData,
    enabled: Boolean = true,
) {
    val toggleAssignTo = remember(inputData) {
        { inputData.isAssignedToOrgMember = !inputData.isAssignedToOrgMember }
    }
    val updateAssignTo =
        remember(inputData) { { b: Boolean -> inputData.isAssignedToOrgMember = b } }
    CrisisCleanupTextCheckbox(
        listItemModifier.listCheckboxAlignStartOffset(),
        inputData.isAssignedToOrgMember,
        text = LocalAppTranslator.current.translator("actions.member_of_my_org"),
        onToggle = toggleAssignTo,
        onCheckChange = updateAssignTo,
        enabled = enabled,
    )
}
