package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.Default
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.feature.caseeditor.model.DetailsInputData
import com.crisiscleanup.feature.caseeditor.model.EmptyFormFieldNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class EditCaseDetailsViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    resourceProvider: AndroidResourceProvider,
    translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    @Dispatcher(Default) private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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

    fun onSystemBack() = validateSaveWorksite()

    fun onNavigateBack() = validateSaveWorksite()

    fun onNavigateCancel(): Boolean {
        return true
    }
}