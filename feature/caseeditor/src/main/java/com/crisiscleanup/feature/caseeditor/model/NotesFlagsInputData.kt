package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteNote

class NotesFlagsInputData(
    worksite: Worksite,
    private val resourceProvider: AndroidResourceProvider,
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    val notes = mutableStateListOf<WorksiteNote>().also { list ->
        worksite.notes?.let { list.addAll(it) }
    }
    var isHighPriority by mutableStateOf(worksite.hasHighPriorityFlag)
    var isAssignedToOrgMember by mutableStateOf(false)

    private fun isChanged(worksite: Worksite): Boolean {
        return this.notes != worksite.notes ||
                isHighPriority != worksite.hasHighPriorityFlag ||
                isAssignedToOrgMember
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
