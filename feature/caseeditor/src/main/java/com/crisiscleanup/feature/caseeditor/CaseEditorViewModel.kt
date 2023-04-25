package com.crisiscleanup.feature.caseeditor

import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.common.*
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.Default
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.crisiscleanup.feature.caseeditor.navigation.CaseEditorArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

internal const val DetailsFormGroupKey = "property_info"
internal const val WorkFormGroupKey = "work_info"
internal const val HazardsFormGroupKey = "hazards_info"
internal const val VolunteerReportFormGroupKey = "claim_status_report_info"

@HiltViewModel
class CaseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    locationsRepository: LocationsRepository,
    worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    editableWorksiteProvider: EditableWorksiteProvider,
    translator: KeyTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    private val resourceProvider: AndroidResourceProvider,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,

    inputValidator: InputValidator,
    searchWorksitesRepository: SearchWorksitesRepository,
    caseIconProvider: MapCaseIconProvider,
    existingWorksiteSelector: ExistingWorksiteSelector,

    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    addressSearchRepository: AddressSearchRepository,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    @Dispatcher(Default) coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : EditCaseBaseViewModel(editableWorksiteProvider, translator, logger) {
    private val caseEditorArgs = CaseEditorArgs(savedStateHandle)
    private val incidentIdArg = caseEditorArgs.incidentId
    private var worksiteIdArg = caseEditorArgs.worksiteId
    private val isCreateWorksite: Boolean
        get() = worksiteIdArg == null

    val headerTitle = MutableStateFlow("")

    val visibleNoteCount: Int = 2

    private val incidentFieldLookup: StateFlow<Map<String, GroupSummaryFieldLookup>>
    val workTypeGroupChildrenLookup: StateFlow<Map<String, Collection<String>>>

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

    val editingWorksite = editableWorksiteProvider.editableWorksite

    val navigateBack = mutableStateOf(false)
    val promptUnsavedChanges = mutableStateOf(false)
    val promptCancelChanges = mutableStateOf(false)
    val isSavingWorksite = MutableStateFlow(false)

    val editIncidentWorksite = MutableStateFlow(existingWorksiteIdentifierNone)
    private var editIncidentWorksiteJob: Job? = null

    private val dataLoader: CaseEditorDataLoader

    private val editOpenedAt = Clock.System.now()

    init {
        updateHeaderTitle()

        editableWorksiteProvider.reset(incidentIdArg)

        dataLoader = CaseEditorDataLoader(
            isCreateWorksite,
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
            { key -> translate(key) },
            editableWorksiteProvider,
            viewModelScope,
            ioDispatcher,
            logger,
        )

        incidentFieldLookup = dataLoader.incidentFieldLookup
        workTypeGroupChildrenLookup = dataLoader.workTypeGroupChildrenLookup

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

        dataLoader.uiState
            .filter {
                (it as? CaseEditorUiState.WorksiteData)?.isEditable == true &&
                        propertyEditor == null
            }
            .onEach {
                propertyEditor = EditablePropertyDataEditor(
                    editableWorksiteProvider,
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
                locationEditor = EditableLocationDataEditor(
                    editableWorksiteProvider,
                    permissionManager,
                    locationProvider,
                    searchWorksitesRepository,
                    addressSearchRepository,
                    caseIconProvider,
                    resourceProvider,
                    drawableResourceBitmapProvider,
                    existingWorksiteSelector,
                    logger,
                    coroutineDispatcher,
                    ioDispatcher,
                    viewModelScope,
                )

                editIncidentWorksiteJob?.cancel()
                editIncidentWorksiteJob = combine(
                    propertyEditor!!.editIncidentWorksite,
                    locationEditor!!.editIncidentWorksite,
                ) { w0, w1 ->
                    if (w0.isDefined) w0
                    else if (w1.isDefined) w1
                    else w0
                }
                    .distinctUntilChanged()
                    .onEach { identifier ->
                        editIncidentWorksite.value = identifier
                    }
                    .launchIn(viewModelScope)
            }
            .launchIn(viewModelScope)


        editingWorksite
            .onEach { worksite ->
                propertyEditor?.setSteadyStateSearchName()

                locationEditor?.locationInputData?.let { inputData ->
                    if (editableWorksiteProvider.takeAddressChanged()) {
                        inputData.assumeLocationAddressChanges(worksite)
                        // TODO Expand address fields if partially defined
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    val uiState = dataLoader.uiState

    val editSections = dataLoader.editSections

    val isLoading = combine(
        dataLoader.isRefreshingIncident,
        dataLoader.isRefreshingWorksite,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

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
                return@combine formData.keys
                    .asSequence()
                    .filter { incident.workTypeLookup[it] != null }
                    .mapNotNull {
                        if (workTypeGroupChildrenLookup.value.containsKey(it)) it
                        else incident.formFieldLookup[it]?.parentKey
                    }
                    .toSet()
                    .sorted()
                    .toList()
            }
        }
        emptyList()
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    var propertyEditor: CasePropertyDataEditor? = null
    var locationEditor: CaseLocationDataEditor? = null

    private fun updateHeaderTitle(caseNumber: String = "") {
        headerTitle.value = if (caseNumber.isEmpty()) {
            val headerTitleResId =
                if (isCreateWorksite) R.string.create_case
                else R.string.view_case
            resourceProvider.getString(headerTitleResId)
        } else {
            resourceProvider.getString(R.string.view_case_number, caseNumber)
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

    // TODO Refactor and add test coverage
    private fun getChangeWorkTypes(
        workTypeLookup: Map<String, String>,
        initialWorksite: Worksite,
        modifiedWorksite: Worksite,
    ): Pair<List<WorkType>, WorkType?> {
        val worksiteWorkTypes =
            initialWorksite.workTypes.associateBy(WorkType::workTypeLiteral)
        val formWorkTypes = modifiedWorksite.formData!!
            .mapNotNull { workTypeLookup[it.key] }
            .toSet()
            .map {
                val now = Clock.System.now()
                worksiteWorkTypes[it] ?: WorkType(
                    id = 0,
                    createdAt = now,
                    orgClaim = null,
                    nextRecurAt = null,
                    // TODO Does this matter
                    phase = 0,
                    recur = null,
                    statusLiteral = WorkTypeStatus.OpenUnassigned.literal,
                    workTypeLiteral = it,
                )
            }
        val initialWorkTypes = initialWorksite.workTypes.sortedBy(WorkType::workTypeLiteral)
        val modifiedWorkTypes = formWorkTypes.sortedBy(WorkType::workTypeLiteral)
        if (initialWorkTypes == modifiedWorkTypes) {
            return Pair(initialWorksite.workTypes, initialWorksite.keyWorkType)
        }

        val formWorkTypeTypes = formWorkTypes.map(WorkType::workType)
        var keyWorkType = initialWorksite.keyWorkType
        if (keyWorkType == null || !formWorkTypeTypes.contains(keyWorkType.workType)) {
            keyWorkType = formWorkTypes.toMutableList()
                .sortedBy(WorkType::workTypeLiteral)
                .firstOrNull()
        }

        return Pair(formWorkTypes, keyWorkType)
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
                val worksiteData = uiState.value as? CaseEditorUiState.WorksiteData
                val initialWorksite = worksiteData?.worksite
                    ?: return@launch

                // TODO Apply changes from each section

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

                val workTypeLookup = worksiteData.incident.workTypeLookup
                val (workTypes, primaryWorkType) = getChangeWorkTypes(
                    workTypeLookup,
                    initialWorksite,
                    worksite,
                )

                if (primaryWorkType == null) {
                    // TODO Different message (same as web)? Add test coverage.
                    invalidWorksiteInfo.value = InvalidWorksiteInfo(
                        WorksiteSection.WorkType,
                        R.string.incomplete_work_type_info,
                    )
                    showInvalidWorksiteSave.value = true
                    return@launch
                }

                val updatedReportedBy =
                    if (worksite.isNew) worksiteData.orgId else worksite.reportedBy
                val clearWhat3Words = worksite.what3Words?.isNotBlank() == true &&
                        worksite.latitude != initialWorksite.latitude ||
                        worksite.longitude != initialWorksite.longitude
                val updatedWhat3Words = if (clearWhat3Words) "" else worksite.what3Words

                val updatedWorksite = worksite.copy(
                    workTypes = workTypes,
                    keyWorkType = primaryWorkType,
                    reportedBy = updatedReportedBy,
                    updatedAt = Clock.System.now(),
                    what3Words = updatedWhat3Words,
                )

                worksiteIdArg = worksiteChangeRepository.saveWorksiteChange(
                    initialWorksite,
                    updatedWorksite,
                    primaryWorkType,
                    worksiteData.orgId,
                )
                val worksiteId = worksiteIdArg!!

                dataLoader.reloadData(worksiteId)
                worksiteProvider.setEditedLocation(worksite.coordinates())

                syncPusher.appPushWorksite(worksiteId)
            } catch (e: Exception) {
                // TODO Show dialog save failed. Try again. If still fails seek help.
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
        val orgId: Long,
        val isEditable: Boolean,
        val worksite: Worksite,
        val incident: Incident,
        val localWorksite: LocalWorksite?,
        val isLocalSyncToBackend: Boolean?,
        val isTranslationUpdated: Boolean,
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