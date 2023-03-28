package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.fabEdgeSpace
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.EditCaseNotesFlagsViewModel
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.model.getRelativeDate
import java.lang.Integer.min

private val ScreenTitleResId = R.string.notes_flags

@Composable
private fun NoteView(
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

private fun LazyListScope.staticNoteItems(
    notes: List<WorksiteNote>,
    visibleCount: Int,
    modifier: Modifier,
) {
    // TODO Animate on expand/collapse
    val count = min(notes.size, visibleCount)
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
internal fun NotesFlagsSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    translate: (String) -> String = { s -> s },
    collapsedNotesVisibleCount: Int = 3,
) {
    EditCaseSummaryHeader(
        ScreenTitleResId,
        isEditable,
        onEdit,
        modifier,
        horizontalPadding = 0.dp,
    ) {
        val paddingStart = 16.dp
        val paddingEnd = 16.dp
        if (worksite.hasHighPriorityFlag) {
            Text(
                text = stringResource(R.string.high_priority),
                // TODO Common styling dimensions
                modifier = modifier.padding(
                    start = paddingStart.plus(8.dp), end = paddingEnd,
                    bottom = 8.dp, top = 8.dp,
                ),
            )
        }

        val notes = worksite.notes
        if (notes.isNotEmpty()) {
            val isExpandable = notes.size > collapsedNotesVisibleCount
            var isExpanded by remember { mutableStateOf(false) }
            val toggleExpand = remember(worksite) { { isExpanded = !isExpanded } }
            Row(
                Modifier
                    .clickable(
                        enabled = isExpandable,
                        onClick = toggleExpand,
                    )
                    .fillMaxWidth()
                    // TODO Common styling dimensions
                    .padding(
                        start = paddingStart.plus(8.dp), end = paddingEnd,
                        bottom = 16.dp, top = 16.dp,
                    ),
            ) {
                Text(
                    text = translate("formLabels.notes"),
                    Modifier.weight(1f),
                )
                if (isExpandable) {
                    val icon = if (isExpanded) CrisisCleanupIcons.ExpandLess
                    else CrisisCleanupIcons.ExpandMore
                    val textKey = if (isExpanded) "actions.some_notes"
                    else "actions.all_notes"
                    Icon(
                        imageVector = icon,
                        contentDescription = translate(textKey),
                    )
                }
            }

            val visibleCount = if (isExpanded) notes.size
            else collapsedNotesVisibleCount
            // TODO Common styling dimensions
            val noteModifier = Modifier.padding(
                start = paddingStart.plus(16.dp), end = paddingEnd,
                bottom = 4.dp, top = 4.dp,
            )
            for (i in 0 until min(notes.size, visibleCount)) {
                val note = notes[i]
                NoteView(note, noteModifier)
            }
        }
    }
}

@Composable
internal fun EditCaseNotesFlagsRoute(
    viewModel: EditCaseNotesFlagsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        stringResource(ScreenTitleResId),
    ) {
        NotesFlagsView()
    }
}

@Composable
private fun NotesFlagsView(
    viewModel: EditCaseNotesFlagsViewModel = hiltViewModel(),
) {
    var isCreatingNote by remember { mutableStateOf(false) }

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (newNoteFab) = createRefs()

        FlagsInputNotesList()

        FloatingActionButton(
            modifier = Modifier.constrainAs(newNoteFab) {
                end.linkTo(parent.end, margin = fabEdgeSpace)
                bottom.linkTo(parent.bottom, margin = fabEdgeSpace)
            },
            onClick = { isCreatingNote = true },
            shape = CircleShape,
        ) {
            Icon(
                imageVector = CrisisCleanupIcons.AddNote,
                contentDescription = viewModel.translate("caseView.add_note_alt"),
            )
        }
    }

    if (isCreatingNote) {
        val dismissNoteDialog = { isCreatingNote = false }
        val onCreateNote = remember(viewModel) {
            { note: WorksiteNote ->
                if (note.note.isNotBlank()) {
                    viewModel.notesFlagsInputData.notes.add(0, note)
                }
                dismissNoteDialog()
            }
        }
        EditNoteDialog(
            note = WorksiteNote.create(),
            dialogTitle = stringResource(R.string.add_note),
            onSave = onCreateNote,
            onCancel = dismissNoteDialog,
        )
    }
}

@Composable
private fun FlagsInputNotesList(
    viewModel: EditCaseNotesFlagsViewModel = hiltViewModel(),
) {
    val inputData = viewModel.notesFlagsInputData

    val notes by inputData.notesStream.collectAsStateWithLifecycle(emptyList())

    val closeKeyboard = rememberCloseKeyboard(viewModel)
    val lazyListState = rememberLazyListState()
    LazyColumn(
        Modifier.scrollFlingListener(closeKeyboard),
        state = lazyListState,
    ) {
        item(
            key = "high-priority",
            contentType = "item-checkbox",
        ) {
            val toggleHighPriority = remember(inputData) {
                { inputData.isHighPriority = !inputData.isHighPriority }
            }
            val updateHighPriority =
                remember(inputData) { { b: Boolean -> inputData.isHighPriority = b } }
            CrisisCleanupTextCheckbox(
                columnItemModifier,
                inputData.isHighPriority,
                text = viewModel.translate("flag.flag_high_priority"),
                onToggle = toggleHighPriority,
                onCheckChange = updateHighPriority,
            )
        }

        if (inputData.isNewWorksite) {
            item(
                key = "assigned-to-org-member",
                contentType = "item-checkbox",
            ) {
                val toggleAssignTo = remember(inputData) {
                    { inputData.isAssignedToOrgMember = !inputData.isAssignedToOrgMember }
                }
                val updateAssignTo =
                    remember(inputData) { { b: Boolean -> inputData.isAssignedToOrgMember = b } }
                CrisisCleanupTextCheckbox(
                    columnItemModifier,
                    inputData.isAssignedToOrgMember,
                    text = viewModel.translate("actions.member_of_my_org"),
                    onToggle = toggleAssignTo,
                    onCheckChange = updateAssignTo,
                )
            }
        }

        if (notes.isNotEmpty()) {
            item(
                key = "notes-title",
                contentType = "item-notes-title",
            ) {
                Text(
                    text = viewModel.translate("formLabels.notes"),
                    modifier = columnItemModifier,
                )
            }

            staticNoteItems(
                notes,
                notes.size,
                columnItemModifier.padding(8.dp),
            )
        }
    }
}
