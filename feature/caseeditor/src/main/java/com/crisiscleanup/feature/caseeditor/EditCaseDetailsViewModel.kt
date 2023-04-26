package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

internal val excludeDetailsFormFields = setOf("cross_street", "email")

internal class EditableDetailsDataEditor(
    worksiteProvider: EditableWorksiteProvider,
) : EditableFormDataEditor(DetailsFormGroupKey, worksiteProvider, excludeDetailsFormFields)

@HiltViewModel
class EditCaseDetailsViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val editor: FormDataEditor = EditableDetailsDataEditor(worksiteProvider)

    private fun validateSaveWorksite() = editor.validateSaveWorksite()

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}