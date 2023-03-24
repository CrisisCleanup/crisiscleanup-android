package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.*
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteNote
import kotlinx.coroutines.flow.map


class NotesFlagsInputData(
    worksite: Worksite,
    val visibleNoteCount: Int = 3,
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    val isNewWorksite = worksite.isNew

    val notes = mutableStateListOf<WorksiteNote>().also { list ->
        worksite.notes?.let { list.addAll(it) }
    }
    var isHighPriority by mutableStateOf(worksite.hasHighPriorityFlag)
    var isAssignedToOrgMember by mutableStateOf(worksite.isAssignedToOrgMember)

    val notesStream = snapshotFlow { notes.toList() }
    val areNotesExpandable = notesStream.map { it.size > visibleNoteCount }

    private fun isChanged(worksite: Worksite): Boolean {
        return this.notes != worksite.notes ||
                isHighPriority != worksite.hasHighPriorityFlag ||
                isAssignedToOrgMember != worksite.isAssignedToOrgMember
    }

    override fun updateCase() = updateCase(worksiteIn)

    override fun updateCase(worksite: Worksite): Worksite? {
        if (!isChanged(worksite)) {
            return worksite
        }

        val flags = worksite.copyModifiedFlags(
            isHighPriority,
            WorksiteFlag::isHighPriorityFlag,
        ) { WorksiteFlag.highPriority() }

        return worksite.copy(
            notes = notes,
            flags = if (flags?.isNotEmpty() == true) flags else null,
            isAssignedToOrgMember = isAssignedToOrgMember,
        )
    }
}
