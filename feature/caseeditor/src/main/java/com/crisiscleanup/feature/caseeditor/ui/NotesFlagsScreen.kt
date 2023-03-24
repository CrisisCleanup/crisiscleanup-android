package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.EditCaseNotesFlagsViewModel
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.model.getRelativeDate


@Composable
internal fun NotesFlagsSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
) {
    EditCaseSummaryHeader(
        R.string.notes_flags,
        isEditable,
        onEdit,
        modifier,
    ) {
        // TODO
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseNotesFlagsRoute(
    viewModel: EditCaseNotesFlagsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    BackHandler {
        if (viewModel.onSystemBack()) {
            onBackClick()
        }
    }

    val onNavigateBack = remember(viewModel) {
        {
            if (viewModel.onNavigateBack()) {
                onBackClick()
            }
        }
    }
    val onNavigateCancel = remember(viewModel) {
        {
            if (viewModel.onNavigateCancel()) {
                onBackClick()
            }
        }
    }
    Column {
        TopAppBarBackCancel(
            titleResId = R.string.notes_flags,
            onBack = onNavigateBack,
            onCancel = onNavigateCancel,
        )

        NotesFlagsView()
    }
}

@Composable
private fun NotesFlagsView(
    viewModel: EditCaseNotesFlagsViewModel = hiltViewModel(),
) {
    val inputData = viewModel.notesFlagsInputData

    val notes by inputData.notesStream.collectAsStateWithLifecycle(emptyList())
    val areNotesExpandable by inputData.areNotesExpandable.collectAsStateWithLifecycle(false)
    var areNotesExpanded by remember { mutableStateOf(false) }
    val toggleNotesExpand = remember(inputData) { { areNotesExpanded = !areNotesExpanded } }

    val closeKeyboard = rememberCloseKeyboard(viewModel)

    LazyColumn(Modifier.scrollFlingListener(closeKeyboard)) {
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
            val visibleCount = if (areNotesExpanded) notes.size
            else inputData.visibleNoteCount
            noteItems(
                viewModel,
                notes,
                visibleCount,
                areNotesExpandable,
                areNotesExpanded,
                toggleNotesExpand,
            )
        }
    }
}

private fun LazyListScope.noteItems(
    viewModel: EditCaseNotesFlagsViewModel,
    notes: List<WorksiteNote>,
    visibleCount: Int,
    isExpandable: Boolean,
    isExpanded: Boolean,
    toggleExpand: () -> Unit,
) {
    item(
        key = "notes-title",
        contentType = "item-notes-title",
    ) {
        Row(
            Modifier
                .clickable(
                    enabled = isExpandable,
                    onClick = toggleExpand,
                )
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = viewModel.translate("formLabels.notes"),
                Modifier.weight(1f),
            )
            if (isExpandable) {
                val icon = if (isExpanded) CrisisCleanupIcons.ExpandMore
                else CrisisCleanupIcons.ExpandLess
                val textKey = if (isExpanded) "actions.some_notes"
                else "actions.all_notes"
                Icon(
                    imageVector = icon,
                    contentDescription = viewModel.translate(textKey),
                )
            }
        }
    }

    // TODO Animate on expand/collapse
    items(
        visibleCount,
        key = {
            val note = notes[it]
            if (note.id > 0) note.id
            else note.createdAt.toEpochMilliseconds()
        },
        contentType = { "item-note" },
    ) {
        val note = notes[it]
        Column(columnItemModifier) {
            // TODO Different styling when isSurvivor and not
            Text(text = note.getRelativeDate())
            Text(text = note.note)
        }
    }
}
