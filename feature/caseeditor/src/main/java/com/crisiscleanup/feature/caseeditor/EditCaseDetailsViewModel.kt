package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.feature.caseeditor.model.DetailsInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

internal val excludeDetailsFormFields = setOf("cross_street", "email")

interface CaseDetailsDataEditor {
    val detailsInputData: DetailsInputData

    fun validateSaveWorksite(): Boolean
}

class EditableDetailsDataEditor(
    private val worksiteProvider: EditableWorksiteProvider,
) : CaseDetailsDataEditor {
    override val detailsInputData: DetailsInputData

    init {
        val groupNode = worksiteProvider.getGroupNode(DetailsFormGroupKey)

        val worksite = worksiteProvider.editableWorksite.value

        detailsInputData = DetailsInputData(
            worksite,
            groupNode,
            excludeDetailsFormFields,
        )
    }

    override fun validateSaveWorksite(): Boolean {
        val updatedWorksite = detailsInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }
}

@HiltViewModel
class EditCaseDetailsViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val editor = EditableDetailsDataEditor(worksiteProvider)

    private fun validateSaveWorksite() = editor.validateSaveWorksite()

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}