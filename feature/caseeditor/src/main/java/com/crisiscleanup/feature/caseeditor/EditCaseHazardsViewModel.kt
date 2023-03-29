package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.feature.caseeditor.model.HazardsInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditCaseHazardsViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val hazardsInputData: HazardsInputData

    init {
        val groupNode = worksiteProvider.getGroupNode(HazardsFormGroupKey)

        val worksite = worksiteProvider.editableWorksite.value

        hazardsInputData = HazardsInputData(
            worksite,
            groupNode,
        )
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = hazardsInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}