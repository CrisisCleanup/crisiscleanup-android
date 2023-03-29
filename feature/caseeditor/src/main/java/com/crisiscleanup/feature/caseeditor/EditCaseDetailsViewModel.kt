package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.feature.caseeditor.model.DetailsInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

internal val excludeDetailsFormFields = setOf("cross_street", "email")

@HiltViewModel
class EditCaseDetailsViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val detailsInputData: DetailsInputData

    init {
        val groupNode = worksiteProvider.getGroupNode(DetailsFormGroupKey)

        val worksite = worksiteProvider.editableWorksite.value

        detailsInputData = DetailsInputData(
            worksite,
            groupNode,
            excludeDetailsFormFields,
        )
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = detailsInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}