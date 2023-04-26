package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

internal class EditableWorkDataEditor(
    worksiteProvider: EditableWorksiteProvider,
) : EditableFormDataEditor(WorkFormGroupKey, worksiteProvider)

@HiltViewModel
class EditCaseWorkViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val editor: FormDataEditor = EditableWorkDataEditor(worksiteProvider)

    private fun validateSaveWorksite() = editor.validateSaveWorksite()

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}