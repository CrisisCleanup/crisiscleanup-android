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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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
    private val workTypeGroupChildrenLookup: StateFlow<Map<String, Collection<String>>>

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

    /**
     * For preventing unwanted editor reloads
     *
     * Editors should be set only "once" during an editing session.
     * External data change signals should be ignored once editing begins or input data may be lost.
     * Managing state internally is much cleaner than weaving and managing related external state.
     */
    private var editorSetInstant: Instant? = null

    /**
     * A sufficient amount of time for local storage to commit and publish network data
     */
    private val editorSetWindow = 10.seconds
    private val caseEditors: StateFlow<CaseEditors?>

    val propertyEditor: CasePropertyDataEditor?
        get() = caseEditors.value?.property
    val locationEditor: CaseLocationDataEditor?
        get() = caseEditors.value?.location
    val notesFlagsEditor: CaseNotesFlagsDataEditor?
        get() = caseEditors.value?.notesFlags
    val formDataEditors: List<FormDataEditor>
        get() = caseEditors.value?.formData ?: emptyList()
    private val workEditor: EditableFormDataEditor?
        get() = caseEditors.value?.work
    private val caseDataWriters: List<CaseDataWriter>
        get() = caseEditors.value?.dataWriters ?: emptyList()

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

        caseEditors = dataLoader.uiState
            .filter {
                (it as? CaseEditorUiState.WorksiteData)?.isNetworkLoadFinished == true &&
                        editorSetInstant?.let { setInstant ->
                            Clock.System.now().minus(setInstant) < editorSetWindow
                        } ?: true
            }
            .mapLatest {
                editorSetInstant = Clock.System.now()

                val propertyEditor = EditablePropertyDataEditor(
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
                val locationEditor = EditableLocationDataEditor(
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
                val notesFlagsEditor = EditableNotesFlagsDataEditor(editableWorksiteProvider)
                val detailsEditor = EditableDetailsDataEditor(editableWorksiteProvider)
                val workEditor = EditableWorkDataEditor(editableWorksiteProvider)
                val hazardsEditor = EditableHazardsDataEditor(editableWorksiteProvider)
                val volunteerReportEditor =
                    EditableVolunteerReportDataEditor(editableWorksiteProvider)

                editIncidentWorksiteJob?.cancel()
                editIncidentWorksiteJob = combine(
                    propertyEditor.editIncidentWorksite,
                    locationEditor.editIncidentWorksite,
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

                CaseEditors(
                    propertyEditor,
                    locationEditor,
                    notesFlagsEditor,
                    details = detailsEditor,
                    work = workEditor,
                    hazards = hazardsEditor,
                    volunteerReport = volunteerReportEditor,
                )
            }
            .stateIn(
                scope = viewModelScope,
                initialValue = null,
                started = SharingStarted.WhileSubscribed(),
            )

        combine(
            caseEditors,
            editingWorksite,
            ::Pair,
        )
            .onEach { (editors, worksite) ->
                editors?.property?.setSteadyStateSearchName()

                editors?.location?.locationInputData?.let { inputData ->
                    if (editableWorksiteProvider.takeAddressChanged()) {
                        inputData.assumeLocationAddressChanges(worksite, true)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    val uiState = dataLoader.uiState

    val editSections = dataLoader.editSections

    val isLoading = dataLoader.isLoading

    val isSyncing = worksiteChangeRepository.syncingWorksiteIds.mapLatest {
        it.contains(worksiteIdArg)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    // Every change matters. This must NOT be distinct.
    val areEditorsReady = caseEditors.mapLatest { it != null }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
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

                editorSetInstant = null
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
            (workEditor as? EditableWorkDataEditor)?.let { workDataEditor ->
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
                worksite = workDataEditor.transferWorkTypes(workTypeLookup, worksite!!)

                worksiteProvider.editableWorksite.value = worksite!!
            }
        }

        return true
    }

    /**
     * @return true if prompt is shown or false if there are no changes
     */
    private fun promptUnsaved(): Boolean {
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

    private fun onBackNavigate(promptOnChange: Boolean = false): Boolean {
        if (isSavingWorksite.value) {
            return false
        }

        return !promptOnChange || !promptUnsaved()
    }

    override fun onSystemBack() = onBackNavigate()

    override fun onNavigateBack() = onBackNavigate(true)

    override fun onNavigateCancel() = onBackNavigate()
}

sealed interface CaseEditorUiState {
    object Loading : CaseEditorUiState

    data class WorksiteData(
        val orgId: Long,
        val isEditCapable: Boolean,
        val statusOptions: List<WorkTypeStatus>,
        val worksite: Worksite,
        val incident: Incident,
        val localWorksite: LocalWorksite?,
        val isNetworkLoadFinished: Boolean,
        val isLocalLoadFinished: Boolean,
        val isTranslationUpdated: Boolean,
    ) : CaseEditorUiState {
        val isPendingSync = !isLocalLoadFinished ||
                localWorksite?.localChanges?.isLocalModified ?: false
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

internal data class CaseEditors(
    val property: EditablePropertyDataEditor,
    val location: EditableLocationDataEditor,
    val notesFlags: EditableNotesFlagsDataEditor,
    val details: EditableDetailsDataEditor,
    val work: EditableWorkDataEditor,
    val hazards: EditableHazardsDataEditor,
    val volunteerReport: EditableVolunteerReportDataEditor,
    val formData: List<FormDataEditor> = listOf(
        details,
        work,
        hazards,
        volunteerReport
    ),
) {
    val dataWriters: List<CaseDataWriter> = mutableListOf(
        property.propertyInputData,
        location.locationInputData,
        notesFlags.notesFlagsInputData,
    ).apply {
        addAll(formData.map(FormDataEditor::inputData))
    }
}