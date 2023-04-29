package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.util.updateWorkTypeStatuses
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

internal class EditableWorkDataEditor(
    worksiteProvider: EditableWorksiteProvider,
) : EditableFormDataEditor(WorkFormGroupKey, worksiteProvider, isWorkInputData = true) {
    fun transferWorkTypes(
        workTypeLookup: Map<String, String>,
        worksite: Worksite,
        createdAt: Instant = Clock.System.now(),
    ) = updateWorkTypeStatuses(workTypeLookup, worksite, inputData, createdAt)
}

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