package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger

open class EditCaseBaseViewModel constructor(
    protected val worksiteProvider: EditableWorksiteProvider,
    private val translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) protected val logger: AppLogger,
) : ViewModel() {
    fun translate(key: String, fallback: String? = null): String {
        return translator.translate(key) ?: (
                worksiteProvider.formFieldTranslationLookup[key]
                    ?: (fallback ?: key)
                )
    }
}