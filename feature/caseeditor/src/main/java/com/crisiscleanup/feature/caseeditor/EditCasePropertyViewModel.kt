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
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.feature.caseeditor.model.ExistingCaseLocation
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditCasePropertyViewModel @Inject constructor(
    worksiteProvider: EditableWorksiteProvider,
    inputValidator: InputValidator,
    resourceProvider: AndroidResourceProvider,
    searchWorksitesRepository: SearchWorksitesRepository,
    caseIconProvider: MapCaseIconProvider,
    translator: KeyTranslator,
    private val existingWorksiteSelector: ExistingWorksiteSelector,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
) : EditCaseBaseViewModel(worksiteProvider, translator, logger) {
    val propertyInputData: PropertyInputData

    private val nameSearchManager: ResidentNameSearchManager
    val searchResults: StateFlow<ResidentNameSearchResults>

    val editIncidentWorksite = existingWorksiteSelector.selected

    private val contactFrequencyOptionValues = listOf(
        AutoContactFrequency.Often,
        AutoContactFrequency.NotOften,
        AutoContactFrequency.Never,
    )

    val contactFrequencyOptions = translator.translationCount.map {
        contactFrequencyOptionValues.map {
            Pair(it, translator.translate(it.literal) ?: it.literal)
        }
    }.stateIn(
        scope = viewModelScope,
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
            scope = viewModelScope,
            initialValue = ResidentNameSearchResults("", emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )
    }

    private fun validateSaveWorksite(): Boolean {
        val updatedWorksite = propertyInputData.updateCase()
        if (updatedWorksite != null) {
            worksiteProvider.editableWorksite.value = updatedWorksite
            return true
        }
        return false
    }

    fun stopSearchingWorksites() = nameSearchManager.stopSearchingWorksites()

    fun onExistingWorksiteSelected(caseLocation: ExistingCaseLocation) {
        viewModelScope.launch(ioDispatcher) {
            existingWorksiteSelector.onNetworkWorksiteSelected(caseLocation.networkWorksiteId)
        }
    }

    override fun onSystemBack() = validateSaveWorksite()

    override fun onNavigateBack() = validateSaveWorksite()
}