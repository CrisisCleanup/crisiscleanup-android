package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.Default
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.LocalWorksite
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.model.CaseDataWriter
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import com.crisiscleanup.feature.caseeditor.model.LocationInputData
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.crisiscleanup.feature.caseeditor.navigation.CaseEditorArgs
import com.crisiscleanup.feature.caseeditor.util.updateKeyWorkType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    incidentBoundsProvider: IncidentBoundsProvider,
    worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    editableWorksiteProvider: EditableWorksiteProvider,
    private val incidentSelector: IncidentSelector,
    private val translator: KeyResourceTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    networkMonitor: NetworkMonitor,
    private val resourceProvider: AndroidResourceProvider,
    appEnv: AppEnv,
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
    private var worksiteIdArg = caseEditorArgs.worksiteId
    private val isCreateWorksite: Boolean
        get() = worksiteIdArg == null

    val headerTitle = MutableStateFlow("")

    val visibleNoteCount: Int = 3

    val isOnline = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(),
        )

    private val incidentFieldLookup: StateFlow<Map<String, GroupSummaryFieldLookup>>
    private val workTypeGroupChildrenLookup: StateFlow<Map<String, Collection<String>>>

    val showInvalidWorksiteSave = MutableStateFlow(false)
    val invalidWorksiteInfo = mutableStateOf(InvalidWorksiteInfo())

    private val editingWorksite = editableWorksiteProvider.editableWorksite

    val navigateBack = mutableStateOf(false)
    val promptUnsavedChanges = mutableStateOf(false)
    val promptCancelChanges = mutableStateOf(false)
    val isSavingWorksite = MutableStateFlow(false)

    val editIncidentWorksite = MutableStateFlow(ExistingWorksiteIdentifierNone)
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
     * A sufficient amount of time for local load to finish after network load has finished.
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

    val changeWorksiteIncidentId = MutableStateFlow(EmptyIncident.id)
    val changeExistingWorksite = MutableStateFlow(ExistingWorksiteIdentifierNone)
    private var saveChangeIncident = EmptyIncident
    private val changingIncidentWorksite: Worksite

    init {
        updateHeaderTitle()

        val incidentIdIn = caseEditorArgs.incidentId

        val incidentChangeData = editableWorksiteProvider.takeIncidentChanged()
        changingIncidentWorksite = incidentChangeData?.worksite ?: EmptyWorksite

        editableWorksiteProvider.reset(incidentIdIn)

        dataLoader = CaseEditorDataLoader(
            isCreateWorksite,
            incidentIdIn,
            worksiteIdArg,
            accountDataRepository,
            incidentsRepository,
            incidentRefresher,
            incidentBoundsProvider,
            worksitesRepository,
            worksiteChangeRepository,
            languageRepository,
            languageRefresher,
            workTypeStatusRepository,
            { key -> translate(key) },
            editableWorksiteProvider,
            resourceProvider,
            viewModelScope,
            ioDispatcher,
            appEnv,
            logger,
        )

        incidentFieldLookup = dataLoader.incidentFieldLookup
        workTypeGroupChildrenLookup = dataLoader.workTypeGroupChildrenLookup

        dataLoader.worksiteStream
            .onEach {
                it?.let { cachedWorksite ->
                    worksitesRepository.setRecentWorksite(
                        incidentIdIn,
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
                // TODO Redesign after worksite caching is complete.
                //      Network load finish should populate data in readonly mode.
                //      Local load finish (needs to be more reliable) enables editable.
                //      Local load must include propagation of database changes and not just local state propagation which is why a delay is necessary.
                //      See data loader state, logic here, and screen's isEditable flag.
                it.asCaseData()?.isNetworkLoadFinished == true &&
                        editorSetInstant?.let { setInstant ->
                            Clock.System.now().minus(setInstant) < editorSetWindow
                        } ?: true
            }
            .mapLatest {
                editorSetInstant = Clock.System.now()

                if (changingIncidentWorksite != EmptyWorksite) {
                    editableWorksiteProvider.editableWorksite.value = changingIncidentWorksite
                }

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
                    viewModelScope,
                )
                val locationEditor = EditableLocationDataEditor(
                    editableWorksiteProvider,
                    permissionManager,
                    locationProvider,
                    incidentBoundsProvider,
                    searchWorksitesRepository,
                    addressSearchRepository,
                    caseIconProvider,
                    resourceProvider,
                    drawableResourceBitmapProvider,
                    existingWorksiteSelector,
                    translator,
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
            editableWorksiteProvider.incidentIdChange,
            ::Triple,
        )
            .onEach { (editors, worksite, _) ->
                editors?.property?.setSteadyStateSearchName()

                editors?.location?.locationInputData?.let { inputData ->
                    if (editableWorksiteProvider.takeAddressChanged()) {
                        inputData.assumeLocationAddressChanges(worksite, true)
                    }

                    editableWorksiteProvider.peekIncidentChange?.let { changeData ->
                        val incidentChangeId = changeData.incident.id
                        if (incidentChangeId != EmptyIncident.id &&
                            incidentChangeId != incidentIdIn
                        ) {
                            onIncidentChange(
                                inputData,
                                changeData.incident,
                                changeData.worksite,
                            )
                        }
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
            if (isCreateWorksite) translate("casesVue.new_case")
            else translate("nav.work_view_case")
        } else {
            resourceProvider.getString(R.string.view_case_number, caseNumber)
        }
    }

    private fun List<String>.translateJoin() = joinToString("\n") { s -> translator(s) }

    private val incompletePropertyInfo = InvalidWorksiteInfo(
        WorksiteSection.Property,
        // TODO Include messages only where applicable
        message = listOf(
            "caseForm.name_required",
            "caseForm.phone_required"
        ).translateJoin()
    )

    private val incompleteLocationInfo = InvalidWorksiteInfo(
        WorksiteSection.Location,
        // TODO Include messages only where applicable
        message = listOf(
            // TODO Missing lat/lng shows the following error
            // caseForm.no_lat_lon_error
            "caseForm.address_required",
            "caseForm.county_required",
            "caseForm.city_required",
            "caseForm.postal_code_required",
            "caseForm.state_required",
        ).translateJoin()
    )

    private fun incompleteFormDataInfo(writerIndex: Int) = InvalidWorksiteInfo(
        when (writerIndex) {
            3 -> WorksiteSection.Details
            4 -> WorksiteSection.WorkType
            5 -> WorksiteSection.Hazards
            6 -> WorksiteSection.VolunteerReport
            else -> WorksiteSection.None
        },
        message = translator("info.missing_required_fields", R.string.incomplete_required_data),
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

    private fun tryGetEditorState() = uiState.value.asCaseData()

    private suspend fun onIncidentChange(
        inputData: LocationInputData,
        changeIncident: Incident,
        addressChangeWorksite: Worksite,
    ) = with(worksiteProvider) {
        // Assume any address changes first before copying
        inputData.assumeLocationAddressChanges(addressChangeWorksite, true)

        val copiedWorksite = copyChanges()
        if (copiedWorksite != EmptyWorksite) {
            if (copiedWorksite.isNew) {
                worksiteProvider.updateIncidentChangeWorksite(copiedWorksite)
                changeWorksiteIncidentId.value = changeIncident.id
                incidentSelector.setIncident(changeIncident)
            } else {
                takeIncidentChanged()

                saveChangeIncident = changeIncident
                saveChanges(false, backOnSuccess = false)
            }
        }
    }

    fun saveChanges(
        claimUnclaimed: Boolean,
        backOnSuccess: Boolean = true,
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
                val editorStateData = tryGetEditorState()
                val initialWorksite = editorStateData?.worksite
                    ?: return@launch

                val worksite = worksiteProvider.editableWorksite.value
                    .updateKeyWorkType(initialWorksite)
                val saveIncidentId = saveChangeIncident.id
                val isIncidentChange = saveIncidentId != EmptyIncident.id &&
                        saveIncidentId != worksite.incidentId
                if (worksite == initialWorksite && !isIncidentChange) {
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
                }

                val updatedIncidentId =
                    if (isIncidentChange) saveIncidentId else worksite.incidentId
                val updatedReportedBy =
                    if (worksite.isNew) editorStateData.orgId else worksite.reportedBy
                val clearWhat3Words = worksite.what3Words?.isNotBlank() == true &&
                        worksite.latitude != initialWorksite.latitude ||
                        worksite.longitude != initialWorksite.longitude
                val updatedWhat3Words = if (clearWhat3Words) "" else worksite.what3Words

                val updatedWorksite = worksite.copy(
                    incidentId = updatedIncidentId,
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

                worksiteProvider.setEditedLocation(worksite.coordinates())
                if (isIncidentChange) {
                    incidentSelector.setIncident(saveChangeIncident)
                } else {
                    editorSetInstant = null
                    dataLoader.reloadData(worksiteId)
                }

                syncPusher.appPushWorksite(worksiteId)

                if (isIncidentChange) {
                    changeExistingWorksite.value =
                        ExistingWorksiteIdentifier(saveIncidentId, worksiteId)
                } else if (backOnSuccess) {
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
        tryGetEditorState()?.let {
            worksiteProvider.editableWorksite.value = it.worksite
        }

        navigateBack.value = true
    }

    private fun transferChanges(indicateInvalidSection: Boolean = false): Boolean {
        tryGetEditorState()?.let { editorState ->
            (workEditor as? EditableWorkDataEditor)?.let { workDataEditor ->
                val initialWorksite = editorState.worksite
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

                val workTypeLookup = editorState.incident.workTypeLookup
                worksite = workDataEditor.transferWorkTypes(workTypeLookup, worksite!!)

                worksiteProvider.editableWorksite.value = worksite!!
            }
        }

        return true
    }

    private fun copyChanges(): Worksite {
        tryGetEditorState()?.let { editorState ->
            var worksite = caseDataWriters.fold(editorState.worksite) { acc, dataWriter ->
                dataWriter.copyCase(acc)
            }
            (workEditor as? EditableWorkDataEditor)?.let { workDataEditor ->
                val workTypeLookup = editorState.incident.workTypeLookup
                worksite = workDataEditor.transferWorkTypes(workTypeLookup, worksite)
            }
            return worksite
        }

        return EmptyWorksite
    }

    /**
     * @return true if prompt is shown or false if there are no changes
     */
    private fun promptUnsaved(): Boolean {
        if (!transferChanges()) {
            promptUnsavedChanges.value = true
            return true
        }

        tryGetEditorState()?.let {
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

    data class CaseData(
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

fun CaseEditorUiState.asCaseData() = this as? CaseEditorUiState.CaseData

data class GroupSummaryFieldLookup(
    val fieldMap: Map<String, String>,
    val optionTranslations: Map<String, String>,
)

data class InvalidWorksiteInfo(
    val invalidSection: WorksiteSection = WorksiteSection.None,
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