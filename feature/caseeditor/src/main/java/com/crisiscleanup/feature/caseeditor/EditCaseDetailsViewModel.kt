package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.feature.caseeditor.model.DetailsInputData
import com.crisiscleanup.feature.caseeditor.model.EmptyFormFieldNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class EditCaseDetailsViewModel @Inject constructor(
    private val worksiteProvider: EditableWorksiteProvider,
    resourceProvider: AndroidResourceProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(CrisisCleanupDispatchers.Default) private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val detailsInputData: DetailsInputData

    init {
        val groupNode =
            worksiteProvider.formFields.firstOrNull { it.fieldKey == "property_info" }
                ?: EmptyFormFieldNode

        val worksite = worksiteProvider.editableWorksite.value

        detailsInputData = DetailsInputData(
            worksite,
            groupNode,
            resourceProvider,
            setOf("cross_street", "email"),
        )
    }

    private fun validateSaveWorksite(): Boolean {
        return false
    }

    fun onSystemBack() = validateSaveWorksite()

    fun onNavigateBack() = validateSaveWorksite()

    fun onNavigateCancel(): Boolean {
        return true
    }
}