package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.feature.caseeditor.model.NotesFlagsInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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
        )
    }

    override fun validateSaveWorksite(): Boolean {
        val updatedWorksite = notesFlagsInputData.updateCase()
        worksiteProvider.editableWorksite.value = updatedWorksite
        return true
    }
}

@HiltViewModel
class EditCaseNotesFlagsViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val editor: CaseNotesFlagsDataEditor = EditableNotesFlagsDataEditor(worksiteProvider)

    private fun validateSaveWorksite() = editor.validateSaveWorksite()

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}
