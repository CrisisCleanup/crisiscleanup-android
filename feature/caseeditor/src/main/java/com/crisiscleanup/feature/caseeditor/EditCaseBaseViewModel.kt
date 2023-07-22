package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger

abstract class EditCaseBaseViewModel(
    protected val worksiteProvider: EditableWorksiteProvider,
    private val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) protected val logger: AppLogger,
) : ViewModel(), KeyResourceTranslator {
    val breakGlassHint = translator("actions.edit")
    val helpHint = translator("actions.help_alt")

    abstract fun onSystemBack(): Boolean

    abstract fun onNavigateBack(): Boolean

    open fun onNavigateCancel(): Boolean {
        return true
    }

    // KeyResourceTranslator

    override val translationCount = translator.translationCount

    override fun translate(phraseKey: String) = translate(phraseKey, 0)

    override fun translate(phraseKey: String, fallbackResId: Int) =
        worksiteProvider.translate(phraseKey) ?: translator.translate(phraseKey, fallbackResId)
}