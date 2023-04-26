package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

internal class EditableVolunteerReportDataEditor(
    worksiteProvider: EditableWorksiteProvider,
) : EditableFormDataEditor(VolunteerReportFormGroupKey, worksiteProvider)

@HiltViewModel
class EditCaseVolunteerReportViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val editor: FormDataEditor = EditableVolunteerReportDataEditor(worksiteProvider)

    private fun validateSaveWorksite() = editor.validateSaveWorksite()

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}