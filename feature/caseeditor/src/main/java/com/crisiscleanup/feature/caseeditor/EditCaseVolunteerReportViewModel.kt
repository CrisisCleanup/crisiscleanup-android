package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.feature.caseeditor.model.VolunteerReportInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditCaseVolunteerReportViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val volunteerReportInputData: VolunteerReportInputData

    init {
        val groupNode = worksiteProvider.getGroupNode(VolunteerReportFormGroupKey)

        val worksite = worksiteProvider.editableWorksite.value

        volunteerReportInputData = VolunteerReportInputData(
            worksite,
            groupNode,
        )
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = volunteerReportInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}