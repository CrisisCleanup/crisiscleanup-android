package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.feature.caseeditor.navigation.ExistingCaseArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class ExistingCaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    locationsRepository: LocationsRepository,
    worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    private val translator: KeyTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    private val resourceProvider: AndroidResourceProvider,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val caseEditorArgs = ExistingCaseArgs(savedStateHandle)
    private val incidentIdArg = caseEditorArgs.incidentId
    private var worksiteIdArg = caseEditorArgs.worksiteId

    val headerTitle = MutableStateFlow("")

    private val dataLoader: CaseEditorDataLoader

    private val editOpenedAt = Clock.System.now()

    init {
        updateHeaderTitle()

        editableWorksiteProvider.reset(incidentIdArg)

        dataLoader = CaseEditorDataLoader(
            false,
            incidentIdArg,
            worksiteIdArg,
            accountDataRepository,
            incidentsRepository,
            incidentRefresher,
            locationsRepository,
            worksitesRepository,
            worksiteChangeRepository,
            languageRepository,
            languageRefresher,
            workTypeStatusRepository,
            { key -> translate(key) },
            editableWorksiteProvider,
            viewModelScope,
            ioDispatcher,
            logger,
        )

        dataLoader.worksiteStream
            .onEach {
                it?.let { cachedWorksite ->
                    worksitesRepository.setRecentWorksite(
                        incidentIdArg,
                        cachedWorksite.worksite.id,
                        editOpenedAt,
                    )
                }
            }
            .flowOn(ioDispatcher)
            .onEach {
                it?.let { cachedWorksite ->
                    updateHeaderTitle(cachedWorksite.worksite.caseNumber)
                }
            }
            .launchIn(viewModelScope)

    }

    val worksite = dataLoader.worksiteStream
        .filter { it != null }
        .map { it!!.worksite }
        .stateIn(
            scope = viewModelScope,
            initialValue = EmptyWorksite,
            started = SharingStarted.WhileSubscribed(),
        )

    val subTitle = worksite.mapLatest {
        if (it == EmptyWorksite) ""
        else listOf(it.county, it.state)
            .filter { s -> s.isNotBlank() }
            .joinToString(", ")
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    fun translate(key: String, fallback: String? = null): String {
        return translator.translate(key) ?: (
                editableWorksiteProvider.formFieldTranslationLookup[key]
                    ?: (fallback ?: key)
                )
    }

    private fun updateHeaderTitle(caseNumber: String = "") {
        headerTitle.value = if (caseNumber.isEmpty()) resourceProvider.getString(R.string.view_case)
        else resourceProvider.getString(R.string.view_case_number, caseNumber)
    }

    // TODO Queue and debounce saves. Save off view model thread in case is long running.
    //      How to keep worksite state synced?

    fun toggleFavorite() {

    }

    fun toggleHighPriority() {

    }

    fun unassignAll() {

    }

    fun claimAll() {

    }

    fun unclaimAll() {

    }
}