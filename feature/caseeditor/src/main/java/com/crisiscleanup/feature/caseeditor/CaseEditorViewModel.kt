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
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.feature.caseeditor.model.*
import com.crisiscleanup.feature.caseeditor.navigation.CaseEditorArgs
import com.crisiscleanup.feature.caseeditor.util.updateKeyWorkType
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
    workTypeStatusRepository: WorkTypeStatusRepository,
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

    val visibleNoteCount: Int = 3

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

    private val editingWorksite = editableWorksiteProvider.editableWorksite

    val navigateBack = mutableStateOf(false)
    val promptUnsavedChanges = mutableStateOf(false)
    val promptCancelChanges = mutableStateOf(false)
    val isSavingWorksite = MutableStateFlow(false)

    val editIncidentWorksite = MutableStateFlow(existingWorksiteIdentifierNone)
    private var editIncidentWorksiteJob: Job? = null

    private val dataLoader: CaseEditorDataLoader

    private val editOpenedAt = Clock.System.now()

    var propertyEditor: CasePropertyDataEditor? = null
    var locationEditor: CaseLocationDataEditor? = null
    var notesFlagsEditor: CaseNotesFlagsDataEditor? = null
    var formDataEditors = emptyList<FormDataEditor>()
    private var detailsEditor: EditableFormDataEditor? = null
    private var workEditor: EditableFormDataEditor? = null
    private var hazardsEditor: EditableFormDataEditor? = null
    private var volunteerReportEditor: EditableFormDataEditor? = null
    var caseDataWriters = emptyList<CaseDataWriter>()

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
            workTypeStatusRepository,
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
                notesFlagsEditor = EditableNotesFlagsDataEditor(editableWorksiteProvider)
                detailsEditor = EditableDetailsDataEditor(editableWorksiteProvider)
                workEditor = EditableWorkDataEditor(editableWorksiteProvider)
                hazardsEditor = EditableHazardsDataEditor(editableWorksiteProvider)
                volunteerReportEditor = EditableVolunteerReportDataEditor(editableWorksiteProvider)
                formDataEditors = listOf(
                    detailsEditor!!,
                    workEditor!!,
                    hazardsEditor!!,
                    volunteerReportEditor!!,
                )
                caseDataWriters = mutableListOf(
                    propertyEditor!!.propertyInputData,
                    locationEditor!!.locationInputData,
                    notesFlagsEditor!!.notesFlagsInputData,
                ).apply {
                    addAll(formDataEditors.map(FormDataEditor::inputData))
                }

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
                        inputData.assumeLocationAddressChanges(worksite, true)
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

    private val hasChanges = combine(
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

    private fun getWorkTypeGroups(state: CaseEditorUiState, worksite: Worksite): List<String> {
        (state as? CaseEditorUiState.WorksiteData)?.let { stateData ->
            worksite.formData?.let { formData ->
                val incident = stateData.incident
                return formData.keys
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
        return emptyList()
    }

    val worksiteWorkTypeGroups = combine(
        editingWorksite,
        uiState,
    ) { worksite, state -> getWorkTypeGroups(state, worksite) }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

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

    private val incompletePropertyInfo = InvalidWorksiteInfo(
        WorksiteSection.Property,
        R.string.incomplete_property_info,
    )

    private val incompleteLocationInfo = InvalidWorksiteInfo(
        WorksiteSection.Location,
        R.string.incomplete_location_info,
    )

    private fun incompleteFormDataInfo(writerIndex: Int) = InvalidWorksiteInfo(
        when (writerIndex) {
            3 -> WorksiteSection.Details
            4 -> WorksiteSection.WorkType
            5 -> WorksiteSection.Hazards
            6 -> WorksiteSection.VolunteerReport
            else -> WorksiteSection.None
        },
        R.string.incomplete_required_data,
    )

    private fun validate(worksite: Worksite): InvalidWorksiteInfo = with(worksite) {
        if (name.isBlank() ||
            phone1.isBlank()
        ) {
            return incompletePropertyInfo
        }

        if (latitude == 0.0 ||
            longitude == 0.0 ||
            address.isBlank() ||
            postalCode.isBlank() ||
            county.isBlank() ||
            city.isBlank() ||
            state.isBlank()
        ) {
            return incompleteLocationInfo
        }

        if (workTypes.isEmpty() ||
            keyWorkType == null ||
            workTypes.find { it.workType == keyWorkType!!.workType } == null
        ) {
            return InvalidWorksiteInfo(
                WorksiteSection.WorkType,
                message = translate("caseForm.select_work_type_error"),
            )
        }

        return InvalidWorksiteInfo()
    }

    fun saveChanges(
        claimUnclaimed: Boolean,
        claimAll: Boolean = false,
        backOnSuccess: Boolean = true
    ) {
        if (!transferChanges(true)) {
            return
        }

        synchronized(isSavingWorksite) {
            if (isSavingWorksite.value) {
                return
            }
            isSavingWorksite.value = true
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val editorStateData = uiState.value as? CaseEditorUiState.WorksiteData
                val initialWorksite = editorStateData?.worksite
                    ?: return@launch

                val worksite = worksiteProvider.editableWorksite.value
                    .updateKeyWorkType(initialWorksite)
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

                var workTypes = worksite.workTypes
                if (claimUnclaimed) {
                    workTypes = workTypes
                        .map {
                            if (it.orgClaim != null) it
                            else it.copy(orgClaim = editorStateData.orgId)
                        }
                } else if (claimAll) {
                    workTypes = workTypes.map { it.copy(orgClaim = editorStateData.orgId) }
                }

                val updatedReportedBy =
                    if (worksite.isNew) editorStateData.orgId else worksite.reportedBy
                val clearWhat3Words = worksite.what3Words?.isNotBlank() == true &&
                        worksite.latitude != initialWorksite.latitude ||
                        worksite.longitude != initialWorksite.longitude
                val updatedWhat3Words = if (clearWhat3Words) "" else worksite.what3Words

                val updatedWorksite = worksite.copy(
                    workTypes = workTypes,
                    reportedBy = updatedReportedBy,
                    updatedAt = Clock.System.now(),
                    what3Words = updatedWhat3Words,
                )

                worksiteIdArg = worksiteChangeRepository.saveWorksiteChange(
                    initialWorksite,
                    updatedWorksite,
                    updatedWorksite.keyWorkType!!,
                    editorStateData.orgId,
                )
                val worksiteId = worksiteIdArg!!

                dataLoader.reloadData(worksiteId)
                worksiteProvider.setEditedLocation(worksite.coordinates())

                syncPusher.appPushWorksite(worksiteId)

                if (backOnSuccess) {
                    navigateBack.value = true
                }
            } catch (e: Exception) {
                logger.logException(e)

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

    private fun transferChanges(indicateInvalidSection: Boolean = false): Boolean {
        (uiState.value as? CaseEditorUiState.WorksiteData)?.let {
            val initialWorksite = it.worksite
            var worksite: Worksite? = initialWorksite
            caseDataWriters.forEachIndexed { index, dataWriter ->
                worksite = dataWriter.updateCase(worksite!!)
                if (worksite == null) {
                    if (indicateInvalidSection) {
                        when (dataWriter) {
                            is PropertyInputData -> {
                                invalidWorksiteInfo.value = incompletePropertyInfo
                                showInvalidWorksiteSave.value = true
                            }
                            is LocationInputData -> {
                                invalidWorksiteInfo.value = incompleteLocationInfo
                                showInvalidWorksiteSave.value = true
                            }
                            is FormFieldsInputData -> {
                                invalidWorksiteInfo.value = incompleteFormDataInfo(index)
                                showInvalidWorksiteSave.value = true
                            }
                        }
                    }

                    return false
                }
            }

            val workTypeLookup = it.incident.workTypeLookup
            val workDataEditor = workEditor as EditableWorkDataEditor
            worksite = workDataEditor.transferWorkTypes(workTypeLookup, worksite!!)

            worksiteProvider.editableWorksite.value = worksite!!
        }

        return true
    }

    /**
     * @return true if prompt is shown or false if there are no changes
     */
    private fun promptSaveChanges(): Boolean {
        if (!transferChanges()) {
            promptUnsavedChanges.value = true
            return true
        }

        (uiState.value as? CaseEditorUiState.WorksiteData)?.let {
            if (it.worksite != worksiteProvider.editableWorksite.value) {
                promptUnsavedChanges.value = true
                return true
            }
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

    override fun onNavigateCancel() = onBackNavigate()
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
        val statusOptions: List<WorkTypeStatus>,
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
    val message: String = "",
)

enum class WorksiteSection {
    None,
    Property,
    Location,
    Details,
    Hazards,
    VolunteerReport,
    WorkType,
}