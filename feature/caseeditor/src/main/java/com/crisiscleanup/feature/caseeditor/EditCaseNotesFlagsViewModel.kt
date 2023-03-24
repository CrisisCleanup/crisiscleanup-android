package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.feature.caseeditor.model.NotesFlagsInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditCaseNotesFlagsViewModel @Inject constructor(
    private val worksiteProvider: EditableWorksiteProvider,
    resourceProvider: AndroidResourceProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : ViewModel() {
    val notesFlagsInputData: NotesFlagsInputData

    init {
        val worksite = worksiteProvider.editableWorksite.value

        notesFlagsInputData = NotesFlagsInputData(
            worksite,
            resourceProvider,
        )
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = notesFlagsInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    fun onSystemBack() = validateSaveWorksite()

    fun onNavigateBack() = validateSaveWorksite()

    fun onNavigateCancel(): Boolean {
        return true
    }
}