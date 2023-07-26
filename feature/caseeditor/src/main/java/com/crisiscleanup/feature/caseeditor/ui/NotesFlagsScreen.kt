package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.survivorNoteColor
import com.crisiscleanup.core.designsystem.theme.survivorNoteColorNoTransparency
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.core.model.data.hasSurvivorNote
import com.crisiscleanup.feature.caseeditor.model.NotesFlagsInputData
import com.crisiscleanup.feature.caseeditor.model.getRelativeDate

@Composable
internal fun NoteView(
    note: WorksiteNote,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val relativeDate = note.getRelativeDate()
        if (relativeDate.isNotEmpty()) {
            Text(
                text = relativeDate,
                // TODO Common dimensions
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(text = note.note)
    }
}

@Composable
internal fun NoteCardView(
    note: WorksiteNote,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (note.isSurvivor) survivorNoteColorNoTransparency else cardContainerColor
    CardSurface(containerColor) {
        NoteView(note, modifier)
    }
}

internal fun LazyListScope.staticNoteItems(
    notes: List<WorksiteNote>,
    visibleCount: Int,
    modifier: Modifier,
    isCardView: Boolean = false,
) {
    if (notes.hasSurvivorNote) {
        item {
            SurvivorNoteLegend(listItemModifier)
        }
    }

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
        if (isCardView) {
            NoteCardView(note, modifier)
        } else {
            val viewModifier =
                if (note.isSurvivor) Modifier
                    .background(survivorNoteColor)
                    .then(modifier)
                else modifier
            NoteView(note, viewModifier)
        }
    }
}

@Composable
internal fun SurvivorNoteLegend(
    modifier: Modifier = Modifier,
) {
    val translator = LocalAppTranslator.current
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(24.dp)
                .background(survivorNoteColorNoTransparency, CircleShape),
        )
        Text(translator("formLabels.survivor_notes"))
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
        dialogTitle = LocalAppTranslator.current("caseView.add_note"),
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
        text = LocalAppTranslator.current("flag.flag_high_priority"),
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
        text = LocalAppTranslator.current("actions.member_of_my_org"),
        onToggle = toggleAssignTo,
        onCheckChange = updateAssignTo,
        enabled = enabled,
    )
}

@Preview
@Composable
private fun SurvivorNoteLegendPreview() {
    CrisisCleanupTheme {
        Surface {
            SurvivorNoteLegend(listItemModifier)
        }
    }
}