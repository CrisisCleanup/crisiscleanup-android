package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteNote
import kotlinx.coroutines.flow.Flow

class NotesFlagsInputData(
    worksite: Worksite,
    val otherNotes: Flow<List<Pair<String, String>>>,
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    val isNewWorksite = worksite.isNew

    val notes = mutableStateListOf<WorksiteNote>().also { it.addAll(worksite.notes) }
    var isHighPriority by mutableStateOf(worksite.hasHighPriorityFlag)
    var isAssignedToOrgMember by mutableStateOf(worksite.isAssignedToOrgMember)

    val notesStream = snapshotFlow { notes.toList() }

    var editingNote by mutableStateOf("")

    private fun isChanged(notes: List<WorksiteNote>, worksite: Worksite): Boolean {
        return notes != worksite.notes ||
            isHighPriority != worksite.hasHighPriorityFlag ||
            isAssignedToOrgMember != worksite.isAssignedToOrgMember
    }

    override fun updateCase() = updateCase(worksiteIn)

    override fun updateCase(worksite: Worksite): Worksite {
        val notes = notes.toList()
        if (!isChanged(notes, worksite)) {
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

    override fun copyCase(worksite: Worksite) = updateCase(worksite)
}
