package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.util.toBounds
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.feature.caseeditor.model.FormFieldNode
import com.crisiscleanup.feature.caseeditor.model.flatten
import com.crisiscleanup.feature.caseeditor.navigation.CaseEditorArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

internal const val DetailsFormGroupKey = "property_info"
internal const val WorkFormGroupKey = "work_info"
internal const val HazardsFormGroupKey = "hazards_info"
internal const val VolunteerReportFormGroupKey = "claim_status_report_info"

@HiltViewModel
class CaseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    incidentsRepository: IncidentsRepository,
    locationsRepository: LocationsRepository,
    private val worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    resourceProvider: AndroidResourceProvider,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : EditCaseBaseViewModel(editableWorksiteProvider, translator, logger) {
    private val caseEditorArgs = CaseEditorArgs(savedStateHandle)
    private val incidentIdArg = caseEditorArgs.incidentId
    private val worksiteIdArg = caseEditorArgs.worksiteId
    val isCreateWorksite = worksiteIdArg == null

    val headerTitle = MutableStateFlow("")

    val visibleNoteCount: Int = 2

    val incidentFieldLookup = MutableStateFlow(emptyMap<String, Map<String, String>>())

    val detailsFieldLookup: Map<String, String>?
        get() = incidentFieldLookup.value[DetailsFormGroupKey]
    val hazardsFieldLookup: Map<String, String>?
        get() = incidentFieldLookup.value[HazardsFormGroupKey]
    val volunteerReportFieldLookup: Map<String, String>?
        get() = incidentFieldLookup.value[VolunteerReportFormGroupKey]

    private val isRefreshingIncident = MutableStateFlow(false)
    private val isRefreshingWorksite = MutableStateFlow(false)
    val isLoading = combine(
        isRefreshingIncident,
        isRefreshingWorksite,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val editingWorksite = editableWorksiteProvider.editableWorksite

    val navigateBack = mutableStateOf(false)

    private val incidentStream = incidentsRepository.streamIncident(incidentIdArg)
        .mapLatest { it ?: EmptyIncident }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val incidentBoundsStream = incidentStream
        .mapLatest {
            var bounds: IncidentBounds? = null
            it?.locations?.map(IncidentLocation::location)?.let { locationIds ->
                val locations = locationsRepository.getLocations(locationIds).toLatLng()
                bounds = if (locations.isEmpty()) null
                else locations.toBounds()
            }
            bounds
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val worksiteStream = flowOf(worksiteIdArg)
        .flatMapLatest { worksiteId ->
            if (worksiteId == null) flowOf(null)
            else worksitesRepository.streamLocalWorksite(worksiteId)
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val isWorksitePulled = AtomicBoolean(false)
    private val isPullingWorksite = AtomicBoolean(false)
    private val networkWorksiteSync = AtomicReference<Pair<Long, NetworkWorksiteFull>?>(null)
    private val networkWorksiteStream = worksiteStream
        .mapLatest { cachedWorksite ->
            cachedWorksite?.let { localWorksite ->
                val networkId = localWorksite.worksite.networkId
                if (networkId > 0 &&
                    !isWorksitePulled.getAndSet(true)
                ) {
                    isPullingWorksite.set(true)
                    try {
                        refreshWorksite(networkId)
                    } finally {
                        isPullingWorksite.set(false)
                    }
                }
            }
            networkWorksiteSync.get()
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val uiStateViewModel = com.crisiscleanup.core.common.combine(
        incidentStream,
        incidentBoundsStream,
        isRefreshingIncident,
        worksiteStream,
        networkWorksiteStream,
    ) { incident, bounds, pullingIncident, worksite, networkWorksiteSync ->
        Pair(
            Triple(incident, bounds, pullingIncident),
            Pair(worksite, networkWorksiteSync)
        )
    }
        .mapLatest { (first, second) ->
            val (incident, bounds, pullingIncident) = first

            if (incident == null) {
                return@mapLatest CaseEditorUiState.Loading
            }

            if (!pullingIncident && incident.formFields.isEmpty()) {
                logger.logException(Exception("Incident $incidentIdArg is missing form fields when editing worksite $worksiteIdArg"))
                return@mapLatest CaseEditorUiState.Error(R.string.incident_issue_try_again)
            }

            bounds?.let {
                if (it.locations.isEmpty()) {
                    logger.logException(Exception("Incident $incidentIdArg is lacking locations."))
                    return@mapLatest CaseEditorUiState.Error(R.string.incident_issue_try_again)
                }
            }

            val (localWorksite, networkWorksiteSync) = second

            val initialWorksite = localWorksite?.worksite ?: EmptyWorksite.copy(
                incidentId = incidentIdArg,
                autoContactFrequencyT = AutoContactFrequency.Often.literal,
            )

            with(editableWorksiteProvider) {
                this.incident = incident
                if (formFields.isEmpty()) {
                    formFields = FormFieldNode.buildTree(incident.formFields, languageRepository)
                        .map(FormFieldNode::flatten)
                    formFieldTranslationLookup =
                        incident.formFields
                            .filter { it.fieldKey.isNotBlank() && it.label.isNotBlank() }
                            .associate { it.fieldKey to it.label }

                    val localTranslate = { s: String -> translate(s) }
                    incidentFieldLookup.value = formFields.associate { node ->
                        val groupFieldMap = node.children.associate { child ->
                            child.fieldKey to child.formField.getFieldLabel(localTranslate)
                        }
                        node.fieldKey to groupFieldMap
                    }
                }
                editableWorksite.value = initialWorksite
                incidentBounds = bounds ?: DefaultIncidentBounds
            }

            val isLoadFinished =
                isCreateWorksite ||
                        (!pullingIncident &&
                                localWorksite != null && isWorksitePulled.get())
            val isEditable = bounds != null && isLoadFinished
            CaseEditorUiState.WorksiteData(
                isEditable = isEditable,
                initialWorksite,
                incident,
                localWorksite,
                networkWorksiteSync,
            )
        }

    val uiState: MutableStateFlow<CaseEditorUiState> = MutableStateFlow(CaseEditorUiState.Loading)

    init {
        val headerTitleResId =
            if (isCreateWorksite) R.string.create_case
            else R.string.view_case
        headerTitle.value = resourceProvider.getString(headerTitleResId)

        editableWorksiteProvider.reset(incidentIdArg)

        viewModelScope.launch(ioDispatcher) {
            try {
                languageRepository.loadLanguages()
            } catch (e: Exception) {
                logger.logException(e)
            }
        }

        viewModelScope.launch(ioDispatcher) {
            isRefreshingIncident.value = true
            try {
                incidentsRepository.pullIncident(incidentIdArg)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isRefreshingIncident.value = false
            }

            // TODO Query backend for updated locations if incident is recent
        }

        worksiteStream
            .onEach {
                it?.let { cachedWorksite ->
                    val caseNumber = cachedWorksite.worksite.caseNumber
                    headerTitle.value =
                        resourceProvider.getString(R.string.view_case_number, caseNumber)
                }
            }
            .launchIn(viewModelScope)

        uiStateViewModel
            .onEach { uiState.value = it }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshWorksite(networkWorksiteId: Long) {
        isRefreshingWorksite.value = true
        try {
            networkWorksiteSync.set(
                worksitesRepository.syncWorksite(
                    incidentIdArg,
                    networkWorksiteId,
                )
            )

            // TODO How to reliably resolve changes between local and network if exists? Ask to overwrite from network or continue with local? Which may overwrite network when pushed?
            // val isLocalModified = cachedWorksite.localChanges.isLocalModified
        } catch (e: Exception) {
            // TODO This is going to be difficult. Plenty of state for possible change... Show error message that backend has changes not resolved on local?
            logger.logException(e)
        } finally {
            isRefreshingWorksite.value = false
        }
    }

    /**
     * @return true if save is ongoing or false if not and processes can continue
     */
    fun saveChanges(promptSave: Boolean = false): Boolean {
        // TODO Compare current and save if there are changes. Show progress.
        //      Close on successful save.
        //      Alert on fail with option to abandon (should never happen when saving and queueing locally).
        return false
    }

    override fun onSystemBack(): Boolean {
        return !saveChanges(true)
    }

    override fun onNavigateBack(): Boolean {
        return !saveChanges(true)
    }

    override fun onNavigateCancel(): Boolean {
        return true
    }
}

sealed interface CaseEditorUiState {
    object Loading : CaseEditorUiState

    data class WorksiteData(
        val isEditable: Boolean,
        val worksite: Worksite,
        val incident: Incident,
        val localWorksite: LocalWorksite?,
        val networkWorksiteSync: Pair<Long, NetworkWorksiteFull>?,
    ) : CaseEditorUiState {
        val isLocalModified = localWorksite?.localChanges?.isLocalModified ?: false
    }

    data class Error(
        val errorResId: Int = 0,
        val errorMessage: String = "",
    ) : CaseEditorUiState
}
