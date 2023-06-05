package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface CasePropertyDataEditor {
    val propertyInputData: PropertyInputData

    val searchResults: StateFlow<ResidentNameSearchResults>

    val editIncidentWorksite: StateFlow<ExistingWorksiteIdentifier>

    val contactFrequencyOptions: StateFlow<List<Pair<AutoContactFrequency, String>>>

    fun setSteadyStateSearchName()

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
    translator: KeyResourceTranslator,
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
            translator,
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
            ignoreNetworkId = worksite.networkId,
        )
        searchResults = nameSearchManager.searchResults.stateIn(
            scope = coroutineScope,
            initialValue = ResidentNameSearchResults("", emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )
    }

    override fun setSteadyStateSearchName() {
        nameSearchManager.updateSteadyStateName(propertyInputData.residentName.value)
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
