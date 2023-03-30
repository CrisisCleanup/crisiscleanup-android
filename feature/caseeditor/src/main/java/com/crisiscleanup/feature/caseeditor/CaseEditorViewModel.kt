package com.crisiscleanup.feature.caseeditor

import androidx.annotation.StringRes
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
import com.crisiscleanup.core.data.util.NetworkMonitor
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
    private val incidentRefresher: IncidentRefresher,
    locationsRepository: LocationsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val networkMonitor: NetworkMonitor,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
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

    private val incidentFieldLookup = MutableStateFlow(emptyMap<String, GroupSummaryFieldLookup>())
    val workTypeGroupChildrenLookup = MutableStateFlow(emptyMap<String, Collection<String>>())

    val detailsFieldLookup: GroupSummaryFieldLookup?
        get() = incidentFieldLookup.value[DetailsFormGroupKey]
    val workFieldLookup: GroupSummaryFieldLookup?
        get() = incidentFieldLookup.value[WorkFormGroupKey]
    val hazardsFieldLookup: GroupSummaryFieldLookup?
        get() = incidentFieldLookup.value[HazardsFormGroupKey]
    val volunteerReportFieldLookup: GroupSummaryFieldLookup?
        get() = incidentFieldLookup.value[VolunteerReportFormGroupKey]

    val showInvalidWorksiteSave = MutableStateFlow(false)
    val invalidWorksiteInfo = mutableStateOf(InvalidWorksiteInfo())

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
    val promptUnsavedChanges = mutableStateOf(false)
    val promptCancelChanges = mutableStateOf(false)
    val isSavingWorksite = MutableStateFlow(false)

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
                    if (networkMonitor.isOnline.first()) {
                        isPullingWorksite.set(true)
                        try {
                            refreshWorksite(networkId)
                        } finally {
                            isPullingWorksite.set(false)
                        }
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

    private val _uiState = com.crisiscleanup.core.common.combine(
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

            var initialWorksite = localWorksite?.worksite ?: EmptyWorksite.copy(
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

                    workTypeGroupChildrenLookup.value =
                        formFields.firstOrNull { it.fieldKey == WorkFormGroupKey }
                            ?.let { node ->
                                node.children.associate {
                                    it.fieldKey to it.children.map(
                                        FormFieldNode::fieldKey
                                    )
                                }
                            }
                            ?: emptyMap()

                    val localTranslate = { s: String -> translate(s) }
                    incidentFieldLookup.value = formFields.associate { node ->
                        val groupFieldMap = node.children.associate { child ->
                            child.fieldKey to child.formField.getFieldLabel(localTranslate)
                        }
                        val groupOptionsMap = node.children.map(FormFieldNode::options)
                            .flatMap { it.entries }
                            .associate { it.key to it.value }
                        node.fieldKey to GroupSummaryFieldLookup(
                            groupFieldMap,
                            groupOptionsMap,
                        )
                    }
                }

                initialWorksite.formData?.let { formData ->
                    val workTypeGroups = formData.keys
                        .filter { incident.workTypeLookup[it] != null }
                        .mapNotNull { incident.formFieldLookup[it]?.parentKey }
                        .toSet()
                    if (workTypeGroups.isNotEmpty()) {
                        val updatedFormData = formData.toMutableMap()
                        workTypeGroups.onEach {
                            updatedFormData[it] = WorksiteFormValue(true, "", true)
                        }
                        initialWorksite = initialWorksite.copy(
                            formData = updatedFormData,
                        )
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

    val hasChanges = combine(
        editingWorksite,
        uiState,
    ) { worksite, state ->
        var isChanged = false
        (state as? CaseEditorUiState.WorksiteData)?.let { data ->
            isChanged = worksite != data.worksite
        }
        isChanged
    }.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(),
    )
    val worksiteWorkTypeGroups = combine(
        editingWorksite,
        uiState,
    ) { worksite, state ->
        (state as? CaseEditorUiState.WorksiteData)?.let { stateData ->
            worksite.formData?.let { formData ->
                val incident = stateData.incident
                val keys = formData.keys
                    .asSequence()
                    .filter { incident.workTypeLookup[it] != null }
                    .mapNotNull {
                        if (workTypeGroupChildrenLookup.value.containsKey(it)) it
                        else incident.formFieldLookup[it]?.parentKey
                    }
                    .toSet()
                    .filter { workTypeGroupChildrenLookup.value.containsKey(it) }
                    .sorted()
                    .toList()
                logger.logDebug("work types $keys ${formData.keys}")
                return@combine keys
            }
        }
        emptyList()
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        val headerTitleResId =
            if (isCreateWorksite) R.string.create_case
            else R.string.view_case
        headerTitle.value = resourceProvider.getString(headerTitleResId)

        editableWorksiteProvider.reset(incidentIdArg)

        viewModelScope.launch(ioDispatcher) {
            try {
                languageRefresher.pullLanguages()
            } catch (e: Exception) {
                logger.logException(e)
            }
        }

        viewModelScope.launch(ioDispatcher) {
            isRefreshingIncident.value = true
            try {
                incidentRefresher.pullIncident(incidentIdArg)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isRefreshingIncident.value = false
            }
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

        _uiState
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

    private fun validate(worksite: Worksite): InvalidWorksiteInfo {
        if (worksite.name.isBlank() ||
            worksite.phone1.isBlank()
        ) {
            return InvalidWorksiteInfo(
                WorksiteSection.Property,
                R.string.incomplete_property_info,
            )
        }

        if (worksite.latitude == 0.0 ||
            worksite.longitude == 0.0 ||
            worksite.address.isBlank() ||
            worksite.postalCode.isBlank() ||
            worksite.county.isBlank() ||
            worksite.city.isBlank() ||
            worksite.state.isBlank()
        ) {
            return InvalidWorksiteInfo(
                WorksiteSection.Location,
                R.string.incomplete_location_info,
            )
        }

        val workTypeCount = worksiteWorkTypeGroups.value.size
        if (workTypeCount == 0) {
            return InvalidWorksiteInfo(
                WorksiteSection.WorkType,
                R.string.incomplete_work_type_info,
            )
        }

        return InvalidWorksiteInfo()
    }

    fun saveChanges(backOnSuccess: Boolean = true) {
        synchronized(isSavingWorksite) {
            if (isSavingWorksite.value) {
                return
            }
            isSavingWorksite.value = true
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val initialWorksite = (uiState.value as? CaseEditorUiState.WorksiteData)?.worksite
                    ?: return@launch

                val worksite = worksiteProvider.editableWorksite.value
                if (worksite == initialWorksite) {
                    if (backOnSuccess) {
                        navigateBack.value = true
                    }
                    return@launch
                }

                val validation = validate(worksite)
                if (validation.invalidSection != WorksiteSection.None) {
                    invalidWorksiteInfo.value = validation
                    showInvalidWorksiteSave.value = true
                    return@launch
                }

                logger.logDebug(
                    "Save changes in worksite",
                    worksite,
                    "from",
                    initialWorksite
                )
            } finally {
                synchronized(isSavingWorksite) {
                    isSavingWorksite.value = false
                }
            }
        }
    }

    fun abandonChanges() {
        navigateBack.value = true
    }

    /**
     * @return true if prompt is shown or false if there are no changes
     */
    private fun promptSaveChanges(): Boolean {
        if (hasChanges.value) {
            promptUnsavedChanges.value = true
            return true
        }
        return false
    }

    private fun onBackNavigate(): Boolean {
        if (isSavingWorksite.value) {
            return false
        }

        return !promptSaveChanges()
    }

    override fun onSystemBack() = onBackNavigate()

    override fun onNavigateBack() = onBackNavigate()

    override fun onNavigateCancel(): Boolean {
        if (isSavingWorksite.value) {
            return false
        }

        if (hasChanges.value) {
            promptCancelChanges.value = true
            return false
        }
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

data class GroupSummaryFieldLookup(
    val fieldMap: Map<String, String>,
    val optionTranslations: Map<String, String>,
)

data class InvalidWorksiteInfo(
    val invalidSection: WorksiteSection = WorksiteSection.None,
    @StringRes val messageResId: Int = 0,
)

enum class WorksiteSection {
    None,
    Property,
    Location,
    WorkType,
}