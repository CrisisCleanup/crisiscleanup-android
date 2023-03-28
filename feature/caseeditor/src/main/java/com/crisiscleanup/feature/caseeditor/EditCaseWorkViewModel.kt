package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.feature.caseeditor.model.EmptyFormFieldNode
import com.crisiscleanup.feature.caseeditor.model.WorkInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditCaseWorkViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val workInputData: WorkInputData

    init {
        val groupNode =
            worksiteProvider.formFields.firstOrNull { it.fieldKey == "work_info" }
                ?: EmptyFormFieldNode

        val worksite = worksiteProvider.editableWorksite.value

        workInputData = WorkInputData(
            worksite,
            groupNode,
        )
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = workInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}