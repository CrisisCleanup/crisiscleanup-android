package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.feature.caseeditor.model.DetailsInputData
import com.crisiscleanup.feature.caseeditor.model.EmptyFormFieldNode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditCaseDetailsViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val detailsInputData: DetailsInputData

    init {
        val groupNode =
            worksiteProvider.formFields.firstOrNull { it.fieldKey == "property_info" }
                ?: EmptyFormFieldNode

        val worksite = worksiteProvider.editableWorksite.value

        detailsInputData = DetailsInputData(
            worksite,
            groupNode,
            setOf("cross_street", "email"),
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