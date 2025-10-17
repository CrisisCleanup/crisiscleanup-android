package com.crisiscleanup.feature.caseeditor

import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.cameraPermissionGranted
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.Default
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AppPreferencesRepository
import com.crisiscleanup.core.data.repository.IncidentClaimThresholdRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.LocalImageRepository
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksiteImageRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.CaseImage
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.ImageCategory
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.LocalWorksite
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.feature.caseeditor.model.CaseDataWriter
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import com.crisiscleanup.feature.caseeditor.model.LocationInputData
import com.crisiscleanup.feature.caseeditor.model.NotesFlagsInputData
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.crisiscleanup.feature.caseeditor.navigation.CaseEditorArgs
import com.crisiscleanup.feature.caseeditor.util.matchKeyWorkType
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@HiltViewModel
class CreateEditCaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountDataRepository: AccountDataRepository,
    accountDataRefresher: AccountDataRefresher,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    incidentBoundsProvider: IncidentBoundsProvider,
    private val worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    editableWorksiteProvider: EditableWorksiteProvider,
    private val incidentSelector: IncidentSelector,
    private val translator: KeyResourceTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val incidentClaimThresholdRepository: IncidentClaimThresholdRepository,
    localImageRepository: LocalImageRepository,
    private val worksiteImageRepository: WorksiteImageRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val syncPusher: SyncPusher,
    networkMonitor: NetworkMonitor,
    packageManager: PackageManager,
    appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,

    inputValidator: InputValidator,
    searchWorksitesRepository: SearchWorksitesRepository,
    caseIconProvider: MapCaseIconProvider,
    existingWorksiteSelector: ExistingWorksiteSelector,

    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    addressSearchRepository: AddressSearchRepository,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,

    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    @Dispatcher(Default) coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : EditCaseBaseViewModel(editableWorksiteProvider, translator, logger), CaseCameraMediaManager {
    companion object {
        internal suspend fun isOverClaiming(
            orgId: Long,
            startingWorksite: Worksite,
            updatedWorksite: Worksite,
            incidentClaimThresholdRepository: IncidentClaimThresholdRepository,
        ): Boolean {
            val endClaimCount = updatedWorksite.getClaimedCount(orgId)
            val startClaimCount = startingWorksite.getClaimedCount(orgId)
            val deltaClaimCount = endClaimCount - startClaimCount
            return !incidentClaimThresholdRepository.isWithinClaimCloseThreshold(
                updatedWorksite.id,
                deltaClaimCount,
            )
        }
    }

    private val caseEditorArgs = CaseEditorArgs(savedStateHandle)
    private var worksiteIdArg = caseEditorArgs.worksiteId
    val isCreateWorksite: Boolean
        get() = worksiteIdArg == null

    private val hasNewWorksitePhotosImages: Boolean
        get() = caseEditorArgs.worksiteId == null &&
            worksiteImageRepository.hasNewWorksiteImages

    val headerTitle = MutableStateFlow("")

    val visibleNoteCount: Int = 3

    val isOnline = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(),
        )

    val showInvalidWorksiteSave = MutableStateFlow(false)
    val invalidWorksiteInfo = mutableStateOf(InvalidWorksiteInfo())

    override val isMapSatelliteView =
        preferencesRepository.userPreferences.map { it.isMapSatelliteView }

    private val editingWorksite = editableWorksiteProvider.editableWorksite
    val photosWorksiteId: Long
        get() = editingWorksite.value.id

    private val caseMediaManager = CaseMediaManager(
        permissionManager,
        localImageRepository,
        worksiteImageRepository,
        worksiteChangeRepository,
        syncPusher,
        viewModelScope,
        ioDispatcher,
        logger,
    )
    val deletingImageIds: StateFlow<Set<Long>> = caseMediaManager.deletingImageIds

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
     * Initial editing data should be set only "once" during an editing session.
     * External data change signals should be ignored once editing begins or input data may be lost.
     * Managing state internally is much cleaner than weaving and managing related external state.
     */
    private var editorSetInstant: Instant? = null

    /**
     * A sufficient amount of time for local load to finish after network load has finished.
     */
    private val editorSetWindow = 1.seconds
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

    val focusScrollToSection = MutableStateFlow(Triple(0, 0, 0))
    private var onSetMyLocationJob: Job? = null

    val syncingWorksiteImage = caseMediaManager.syncingWorksiteImage

    var addImageCategory by mutableStateOf(ImageCategory.Before)

    override val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    override var showExplainPermissionCamera by mutableStateOf(false)
    override var isCameraPermissionGranted by mutableStateOf(false)

    override val capturePhotoUri: Uri?
        get() = worksiteImageRepository.newPhotoUri

    var isOverClaimingWork by mutableStateOf(false)

    init {
        updateHeaderTitle()

        val incidentIdIn = caseEditorArgs.incidentId

        val incidentChangeData = editableWorksiteProvider.takeIncidentChanged()
        changingIncidentWorksite = incidentChangeData?.worksite ?: EmptyWorksite

        editableWorksiteProvider.reset(incidentIdIn)

        if (isCreateWorksite) {
            worksiteImageRepository.clearNewWorksiteImages()
        }

        dataLoader = CaseEditorDataLoader(
            isCreateWorksite,
            incidentIdIn,
            worksiteIdArg,
            accountDataRepository,
            accountDataRefresher,
            incidentsRepository,
            incidentRefresher,
            incidentBoundsProvider,
            locationProvider,
            worksitesRepository,
            worksiteChangeRepository,
            languageRepository,
            languageRefresher,
            workTypeStatusRepository,
            { key -> translate(key) },
            editableWorksiteProvider,
            viewModelScope,
            ioDispatcher,
            appEnv,
            logger,
        )

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

        caseEditors = dataLoader.viewState
            .filter {
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
                    if (w0.isDefined) {
                        w0
                    } else if (w1.isDefined) {
                        w1
                    } else {
                        w0
                    }
                }
                    .distinctUntilChanged()
                    .onEach { identifier ->
                        editIncidentWorksite.value = identifier
                    }
                    .launchIn(viewModelScope)

                onSetMyLocationJob?.cancel()
                onSetMyLocationJob = locationEditor.onSetMyLocationAddress
                    .onEach {
                        if (it != null) {
                            focusScrollToAddressSection()
                        }
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
                        inputData.assumeLocationAddressChanges(worksite)
                        focusScrollToAddressSection()
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

        permissionManager.permissionChanges
            .onEach {
                if (it == cameraPermissionGranted) {
                    caseMediaManager.continueTakePhotoGate.set(true)
                    isCameraPermissionGranted = true
                }
            }
            .launchIn(viewModelScope)
    }

    val viewState = dataLoader.viewState

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

    private val photosLookup = if (isCreateWorksite) {
        worksiteImageRepository.streamNewWorksiteImages().mapToCategoryLookup()
    } else {
        processWorksiteFilesNotes(
            editableWorksiteProvider.editableWorksite,
            viewState,
        )
            .organizeBeforeAfterPhotos()
    }
    val beforeAfterPhotos = photosLookup
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap<ImageCategory, List<CaseImage>>(),
            started = SharingStarted.WhileSubscribed(),
        )

    val incidentCreation = viewState
        .mapNotNull {
            it.asCaseData()?.incident?.startAt?.let { startAt ->
                IncidentCreation(
                    Clock.System.now() - startAt > 180.days,
                    startAt.relativeTime,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = incidentCreationNow,
            started = SharingStarted.WhileSubscribed(),
        )

    override fun setMapSatelliteView(isSatellite: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.setMapSatelliteView(isSatellite)
        }
    }

    fun scheduleSync() {
        val worksite = editingWorksite.value
        if (!(worksite.isNew || isSyncing.value)) {
            syncPusher.appPushWorksite(worksite.id, true)
        }
    }

    private fun updateHeaderTitle(caseNumber: String = "") {
        headerTitle.value = if (caseNumber.isEmpty()) {
            if (isCreateWorksite) {
                translate("casesVue.new_case")
            } else {
                translate("nav.work_view_case")
            }
        } else {
            "${translate("actions.view")} $caseNumber"
        }
    }

    fun clearFocusScrollToSection() {
        focusScrollToSection.value = Triple(0, 0, 0)
    }

    private fun focusScrollToAddressSection() {
        var scrollOffset = focusScrollToSection.value.third
        scrollOffset = if (scrollOffset == 128) 127 else 128
        focusScrollToSection.value = Triple(0, 3, scrollOffset)
    }

    private fun translateInvalidInfo(
        translateKey: String,
        section: WorksiteSection,
    ) = InvalidWorksiteInfo(
        section,
        translate(translateKey),
    )

    private fun translateInvalidAddressInfo(
        translateKey: String,
        section: WorksiteSection = WorksiteSection.LocationAddress,
    ): InvalidWorksiteInfo {
        val invalidSection = if (section == WorksiteSection.LocationAddress &&
            locationEditor?.isSearchSuggested == true
        ) {
            WorksiteSection.Location
        } else {
            WorksiteSection.LocationAddress
        }
        return InvalidWorksiteInfo(
            invalidSection,
            translate(translateKey),
        )
    }

    private fun validate(worksite: Worksite): InvalidWorksiteInfo = with(worksite) {
        if (name.isBlank()) {
            return translateInvalidInfo(
                "caseForm.name_required",
                WorksiteSection.Property,
            )
        }
        if (phone1.isBlank()) {
            return translateInvalidInfo(
                "caseForm.phone_required",
                WorksiteSection.Property,
            )
        }

        if (latitude == 0.0 || longitude == 0.0) {
            return translateInvalidInfo(
                "caseForm.no_lat_lon_error",
                WorksiteSection.Location,
            )
        }
        if (address.isBlank()) {
            return translateInvalidAddressInfo("caseForm.address_required")
        }
        if (postalCode.isBlank()) {
            return translateInvalidAddressInfo("caseForm.postal_code_required")
        }
        if (county.isBlank()) {
            return translateInvalidAddressInfo("caseForm.county_required")
        }
        if (city.isBlank()) {
            return translateInvalidAddressInfo("caseForm.city_required")
        }
        if (state.isBlank()) {
            return translateInvalidAddressInfo("caseForm.state_required")
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

    private fun tryGetEditorState() = viewState.value.asCaseData()

    private suspend fun onIncidentChange(
        inputData: LocationInputData,
        changeIncident: Incident,
        addressChangeWorksite: Worksite,
    ) {
        // TODO Prevent user mutations when ongoing. Test.

        // Assume any address changes first before copying
        inputData.assumeLocationAddressChanges(addressChangeWorksite)

        var copiedWorksite = copyChanges()
        if (copiedWorksite != EmptyWorksite) {
            if (copiedWorksite.isNew) {
                notesFlagsEditor?.let {
                    copiedWorksite = transferEditingNote(it.notesFlagsInputData, copiedWorksite)
                }
                worksiteProvider.updateIncidentChangeWorksite(copiedWorksite)
                changeWorksiteIncidentId.value = changeIncident.id
                incidentSelector.submitIncidentChange(changeIncident)
            } else {
                worksiteProvider.takeIncidentChanged()

                saveChangeIncident = changeIncident
                saveChanges(false, backOnSuccess = false)
            }
        }
    }

    private suspend fun isOverClaiming(
        startingWorksite: Worksite,
        updatedWorksite: Worksite,
    ): Boolean {
        val accountData = accountDataRepository.accountData.first()
        val orgId = accountData.org.id
        return isOverClaiming(
            orgId,
            startingWorksite,
            updatedWorksite,
            incidentClaimThresholdRepository,
        )
    }

    fun saveChanges(
        claimUnclaimed: Boolean,
        backOnSuccess: Boolean = true,
    ) {
        if (!transferChanges(true)) {
            return
        }

        if (!isSavingWorksite.compareAndSet(expect = false, update = true)) {
            return
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val editorStateData = tryGetEditorState()
                val initialWorksite = editorStateData?.worksite
                    ?: return@launch

                val worksite = editingWorksite.value
                    .updateKeyWorkType(initialWorksite)
                val saveIncidentId = saveChangeIncident.id
                val isIncidentChange = saveIncidentId != EmptyIncident.id &&
                    saveIncidentId != worksite.incidentId
                if (worksite == initialWorksite &&
                    !isIncidentChange &&
                    (!claimUnclaimed || worksite.unclaimedCount == 0)
                ) {
                    if (hasNewWorksitePhotosImages) {
                        propertyEditor?.propertyInputData?.let {
                            setInvalidSection(0, it)
                        }
                        return@launch
                    }

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

                var keyWorkType = worksite.keyWorkType
                val orgId = editorStateData.orgId
                var workTypes = worksite.workTypes
                if (claimUnclaimed) {
                    workTypes = workTypes
                        .map {
                            if (it.orgClaim != null) {
                                it
                            } else {
                                it.copy(orgClaim = orgId)
                            }
                        }
                    keyWorkType = workTypes.matchKeyWorkType(initialWorksite)
                }

                val updatedIncidentId =
                    if (isIncidentChange) saveIncidentId else worksite.incidentId
                val updatedReportedBy = if (worksite.isNew) orgId else worksite.reportedBy
                val clearWhat3Words = worksite.what3Words?.isNotBlank() == true &&
                    worksite.latitude != initialWorksite.latitude ||
                    worksite.longitude != initialWorksite.longitude
                val updatedWhat3Words = if (clearWhat3Words) "" else worksite.what3Words

                val updatedWorksite = worksite.copy(
                    incidentId = updatedIncidentId,
                    keyWorkType = keyWorkType,
                    workTypes = workTypes,
                    reportedBy = updatedReportedBy,
                    updatedAt = Clock.System.now(),
                    what3Words = updatedWhat3Words,
                )

                if (!isCreateWorksite &&
                    isOverClaiming(worksite, updatedWorksite)
                ) {
                    isOverClaimingWork = true
                    return@launch
                }

                worksiteIdArg = worksiteChangeRepository.saveWorksiteChange(
                    initialWorksite,
                    updatedWorksite,
                    updatedWorksite.keyWorkType!!,
                    orgId,
                )
                val worksiteId = worksiteIdArg!!

                if (hasNewWorksitePhotosImages) {
                    worksiteImageRepository.transferNewWorksiteImages(worksiteId)
                }

                worksiteProvider.setEditedLocation(worksite.coordinates)
                if (isIncidentChange) {
                    incidentSelector.submitIncidentChange(saveChangeIncident)
                } else {
                    editorSetInstant = null
                    dataLoader.reloadData(worksiteId)
                }

                syncPusher.appPushWorksite(worksiteId, true)

                if (isCreateWorksite) {
                    incidentClaimThresholdRepository.onWorksiteCreated(worksiteId)
                }

                worksitesRepository.setRecentWorksite(
                    incidentId = updatedIncidentId,
                    worksiteId = worksiteId,
                    viewStart = Clock.System.now(),
                )

                if (isIncidentChange) {
                    changeExistingWorksite.value =
                        ExistingWorksiteIdentifier(saveIncidentId, worksiteId)
                } else if (backOnSuccess) {
                    navigateBack.value = true
                }
            } catch (e: Exception) {
                onSaveFail(e)
            } finally {
                synchronized(isSavingWorksite) {
                    isSavingWorksite.value = false
                }
            }
        }
    }

    private fun onSaveFail(
        e: Exception,
        isMediaSave: Boolean = false,
    ) {
        logger.logException(e)

        // TODO Show dialog save failed. Try again. If still fails seek help.
    }

    fun abandonChanges() {
        tryGetEditorState()?.let {
            worksiteProvider.editableWorksite.value = it.worksite
        }

        navigateBack.value = true
    }

    private fun setInvalidSection(index: Int, dataWriter: CaseDataWriter) {
        when (dataWriter) {
            is PropertyInputData -> {
                invalidWorksiteInfo.value = InvalidWorksiteInfo(
                    WorksiteSection.Property,
                    dataWriter.getUserErrorMessage(),
                )
                showInvalidWorksiteSave.value = true
            }

            is LocationInputData -> {
                val (isAddressError, message) = dataWriter.getUserErrorMessage()
                val section =
                    if (locationEditor?.isSearchSuggested == true || !isAddressError) {
                        WorksiteSection.Location
                    } else {
                        WorksiteSection.LocationAddress
                    }
                invalidWorksiteInfo.value = InvalidWorksiteInfo(section, message)
                showInvalidWorksiteSave.value = true
            }

            is FormFieldsInputData -> {
                invalidWorksiteInfo.value = InvalidWorksiteInfo(
                    when (index) {
                        3 -> WorksiteSection.Details
                        4 -> WorksiteSection.WorkType
                        5 -> WorksiteSection.Hazards
                        6 -> WorksiteSection.VolunteerReport
                        else -> WorksiteSection.None
                    },
                    message = translator("caseForm.missing_required_fields"),
                )
                showInvalidWorksiteSave.value = true
            }
        }
    }

    private fun transferEditingNote(
        notesInputData: NotesFlagsInputData,
        worksite: Worksite,
    ): Worksite {
        var updatedWorksite = worksite

        val editingNote = notesInputData.editingNote.trim()
        var notes = worksite.notes
        if (editingNote.isNotBlank() &&
            (notes.isEmpty() || notes.first().note.trim() != editingNote)
        ) {
            notes = notes.toMutableList()
                .also {
                    val note = WorksiteNote.create().copy(note = editingNote)
                    it.add(0, note)
                }
            updatedWorksite = updatedWorksite.copy(notes = notes)
            notesInputData.editingNote = ""
        }
        return updatedWorksite
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
                            setInvalidSection(index, dataWriter)
                        }
                        return false
                    }
                }

                notesFlagsEditor?.let {
                    worksite = transferEditingNote(it.notesFlagsInputData, worksite!!)
                }

                val workTypeLookup = editorState.incident.workTypeLookup
                worksite = workDataEditor.transferWorkTypes(workTypeLookup, worksite!!)

                worksiteProvider.editableWorksite.value = worksite
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
            if (it.worksite != editingWorksite.value) {
                promptUnsavedChanges.value = true
                return true
            }

            if (hasNewWorksitePhotosImages) {
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

    // CaseCameraMediaManager

    override fun takePhoto() = caseMediaManager.takePhoto { showExplainPermissionCamera = true }

    override fun continueTakePhoto() = caseMediaManager.continueTakePhotoGate.getAndSet(false)

    override fun onMediaSelected(uri: Uri, isFileSelected: Boolean) {
        tryGetEditorState()?.let { caseData ->
            caseMediaManager.onMediaSelected(
                caseData.worksite.id,
                addImageCategory.literal,
                uri,
                isFileSelected,
            ) { e -> onSaveFail(e, true) }
        }
    }

    override fun onMediaSelected(uris: List<Uri>) {
        uris.forEach {
            onMediaSelected(it, true)
        }
    }

    override fun onDeleteImage(image: CaseImage) {
        if (isCreateWorksite) {
            worksiteImageRepository.deleteNewWorksiteImage(image.imageUri)
        } else {
            caseMediaManager.deleteImage(image.id, image.isNetworkImage)
        }
    }
}

sealed interface CaseEditorViewState {
    data object Loading : CaseEditorViewState

    data class CaseData(
        val orgId: Long,
        val isEditingAllowed: Boolean,
        val statusOptions: List<WorkTypeStatus>,
        val worksite: Worksite,
        val incident: Incident,
        val localWorksite: LocalWorksite?,
        val isNetworkLoadFinished: Boolean,
        val isLocalLoadFinished: Boolean,
        val isTranslationUpdated: Boolean,
    ) : CaseEditorViewState {
        val isPendingSync = !isLocalLoadFinished ||
            localWorksite?.localChanges?.isLocalModified ?: false
    }

    data class Error(
        val errorResId: Int = 0,
        val errorMessage: String = "",
    ) : CaseEditorViewState
}

fun CaseEditorViewState.asCaseData() = this as? CaseEditorViewState.CaseData

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
    LocationAddress,
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
        volunteerReport,
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

data class IncidentCreation(
    val isOldIncident: Boolean,
    val relativeTime: String,
)

private val incidentCreationNow = IncidentCreation(false, "")
