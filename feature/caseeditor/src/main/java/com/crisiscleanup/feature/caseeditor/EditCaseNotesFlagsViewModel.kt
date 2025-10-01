package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.feature.caseeditor.model.NotesFlagsInputData

interface CaseNotesFlagsDataEditor {
    val notesFlagsInputData: NotesFlagsInputData

    fun validateSaveWorksite(): Boolean
}

internal class EditableNotesFlagsDataEditor(
    private val worksiteProvider: EditableWorksiteProvider,
) : CaseNotesFlagsDataEditor {
    override val notesFlagsInputData: NotesFlagsInputData

    init {
        val worksite = worksiteProvider.editableWorksite.value

        notesFlagsInputData = NotesFlagsInputData(
            worksite,
            worksiteProvider.otherNotes,
        )
    }

    override fun validateSaveWorksite(): Boolean {
        val updatedWorksite = notesFlagsInputData.updateCase()
        worksiteProvider.editableWorksite.value = updatedWorksite
        return true
    }
}
