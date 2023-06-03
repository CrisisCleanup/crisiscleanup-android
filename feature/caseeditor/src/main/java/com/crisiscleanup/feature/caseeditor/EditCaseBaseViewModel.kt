package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger

abstract class EditCaseBaseViewModel(
    protected val worksiteProvider: EditableWorksiteProvider,
    private val translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) protected val logger: AppLogger,
) : ViewModel() {

    val breakGlassHint = translator.translate("actions.edit") ?: ""
    val helpHint = translator.translate("actions.help_alt") ?: ""

    fun translate(key: String, fallback: String? = null) = translator.translate(key)
        ?: (worksiteProvider.translate(key) ?: (fallback ?: key))

    abstract fun onSystemBack(): Boolean

    abstract fun onNavigateBack(): Boolean

    open fun onNavigateCancel(): Boolean {
        return true
    }
}