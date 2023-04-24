package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

interface CasePropertyDataEditor {
    val propertyInputData: PropertyInputData

    val searchResults: StateFlow<ResidentNameSearchResults>

    val editIncidentWorksite: StateFlow<ExistingWorksiteIdentifier>

    val contactFrequencyOptions: StateFlow<List<Pair<AutoContactFrequency, String>>>

    fun stopSearchingWorksites()

    fun onExistingWorksiteSelected(result: CaseSummaryResult)

    fun onBackValidateSaveWorksite(): Boolean
}

internal class EditablePropertyDataEditor(
    private val worksiteProvider: EditableWorksiteProvider,
    inputValidator: InputValidator,
    resourceProvider: AndroidResourceProvider,
    searchWorksitesRepository: SearchWorksitesRepository,
    caseIconProvider: MapCaseIconProvider,
    translator: KeyTranslator,
    private val existingWorksiteSelector: ExistingWorksiteSelector,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: AppLogger,
    private val coroutineScope: CoroutineScope,
) : CasePropertyDataEditor {
    override val propertyInputData: PropertyInputData

    private val nameSearchManager: ResidentNameSearchManager
    override val searchResults: StateFlow<ResidentNameSearchResults>

    override val editIncidentWorksite = existingWorksiteSelector.selected

    private val contactFrequencyOptionValues = listOf(
        AutoContactFrequency.Often,
        AutoContactFrequency.NotOften,
        AutoContactFrequency.Never,
    )

    override val contactFrequencyOptions = translator.translationCount.map {
        contactFrequencyOptionValues.map {
            Pair(it, translator.translate(it.literal) ?: it.literal)
        }
    }.stateIn(
        scope = coroutineScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(),
    )

    init {
        val worksite = worksiteProvider.editableWorksite.value

        propertyInputData = PropertyInputData(
            inputValidator,
            worksite,
            resourceProvider,
        )

        nameSearchManager = ResidentNameSearchManager(
            worksite.incidentId,
            propertyInputData,
            searchWorksitesRepository,
            caseIconProvider,
            ioDispatcher,
        )
        searchResults = nameSearchManager.searchResults.stateIn(
            scope = coroutineScope,
            initialValue = ResidentNameSearchResults("", emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )
    }

    override fun stopSearchingWorksites() = nameSearchManager.stopSearchingWorksites()

    override fun onExistingWorksiteSelected(result: CaseSummaryResult) {
        coroutineScope.launch(ioDispatcher) {
            existingWorksiteSelector.onNetworkWorksiteSelected(result.networkWorksiteId)
        }
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = propertyInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    override fun onBackValidateSaveWorksite(): Boolean {
        if (searchResults.value.isNotEmpty) {
            stopSearchingWorksites()
            return false
        }

        return validateSaveWorksite()
    }
}

@HiltViewModel
class EditCasePropertyViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    inputValidator: InputValidator,
    resourceProvider: AndroidResourceProvider,
    searchWorksitesRepository: SearchWorksitesRepository,
    caseIconProvider: MapCaseIconProvider,
    translator: KeyTranslator,
    existingWorksiteSelector: ExistingWorksiteSelector,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val editor: CasePropertyDataEditor = EditablePropertyDataEditor(
        worksiteProvider,
        inputValidator,
        resourceProvider,
        searchWorksitesRepository,
        caseIconProvider,
        translator,
        existingWorksiteSelector,
        ioDispatcher,
        logger,
        viewModelScope
    )

    override fun onSystemBack() = editor.onBackValidateSaveWorksite()

    override fun onNavigateBack() = editor.onBackValidateSaveWorksite()
}