package com.crisiscleanup.feature.caseeditor

import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.cameraPermissionGranted
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.common.kmToMiles
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.utcTimeZone
import com.crisiscleanup.core.commoncase.TransferWorkTypeProvider
import com.crisiscleanup.core.commoncase.WorkTypeTransferType
import com.crisiscleanup.core.commoncase.oneDecimalFormat
import com.crisiscleanup.core.data.IncidentRefresher
import com.crisiscleanup.core.data.LanguageRefresher
import com.crisiscleanup.core.data.OrganizationRefresher
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.LocalImageRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksiteImageRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.model.data.CaseImage
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.ImageCategory
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeRequest
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.crisiscleanup.feature.caseeditor.navigation.ExistingCaseArgs
import com.google.android.gms.maps.model.BitmapDescriptor
import com.philjay.RRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ViewCaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    private val incidentsRepository: IncidentsRepository,
    organizationsRepository: OrganizationsRepository,
    organizationRefresher: OrganizationRefresher,
    worksiteInteractor: CasesWorksiteInteractor,
    incidentRefresher: IncidentRefresher,
    incidentBoundsProvider: IncidentBoundsProvider,
    locationProvider: LocationProvider,
    worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    accountDataRefresher: AccountDataRefresher,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    localImageRepository: LocalImageRepository,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    val transferWorkTypeProvider: TransferWorkTypeProvider,
    permissionManager: PermissionManager,
    private val translator: KeyResourceTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val worksiteImageRepository: WorksiteImageRepository,
    private val syncPusher: SyncPusher,
    packageManager: PackageManager,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), KeyResourceTranslator, CaseCameraMediaManager {
    private val existingCaseArgs = ExistingCaseArgs(savedStateHandle)
    private val incidentIdArg = existingCaseArgs.incidentId
    val worksiteIdArg = existingCaseArgs.worksiteId

    val headerTitle = MutableStateFlow("")

    private val nextRecurDateFormat =
        DateTimeFormatter.ofPattern("EEE MMMM d yyyy ['at'] h:mm a").utcTimeZone

    private val dataLoader: CaseEditorDataLoader

    private val editOpenedAt = Clock.System.now()

    val mapMarkerIcon = MutableStateFlow<BitmapDescriptor?>(null)
    private var inBoundsPinIcon: BitmapDescriptor? = null
    private var outOfBoundsPinIcon: BitmapDescriptor? = null

    val isSyncing = combine(
        worksiteChangeRepository.syncingWorksiteIds,
        localImageRepository.syncingWorksiteId,
        ::Pair,
    )
        .mapLatest { (syncingWorksiteIds, imageSyncingWorksiteId) ->
            syncingWorksiteIds.contains(worksiteIdArg) ||
                imageSyncingWorksiteId == worksiteIdArg
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

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

    private val isSavingWorksite = MutableStateFlow(false)
    val isSaving = combine(
        isSavingWorksite,
        caseMediaManager.isSavingMedia,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val syncingWorksiteImage = caseMediaManager.syncingWorksiteImage

    private var isOrganizationsRefreshed = AtomicBoolean(false)
    private val organizationLookup = organizationsRepository.organizationLookup
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(),
        )

    val editableWorksite = editableWorksiteProvider.editableWorksite
    val updatedAtText = editableWorksite.map { worksite ->
        worksite.updatedAt?.let {
            return@map translator("caseView.updated_ago")
                .replace("{relative_time}", it.relativeTime)
        }
        ""
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val distanceAwayText = editableWorksite.map { worksite ->
        locationProvider.getLocation()?.let { (latitude, longitude) ->
            val worksiteLatRad = worksite.latitude.radians
            val worksiteLngRad = worksite.longitude.radians
            val latRad = latitude.radians
            val lngRad = longitude.radians
            val distanceAwayMi = haversineDistance(
                latRad,
                lngRad,
                worksiteLatRad,
                worksiteLngRad,
            ).kmToMiles
            val distanceAwayText = oneDecimalFormat.format(distanceAwayMi)
            return@map "$distanceAwayText ${translator("caseView.miles_abbrv")}"
        }

        ""
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val jumpToCaseOnMapOnBack = MutableStateFlow(false)

    var addImageCategory by mutableStateOf(ImageCategory.Before)

    override val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    override var showExplainPermissionCamera by mutableStateOf(false)
    override var isCameraPermissionGranted by mutableStateOf(false)

    override val capturePhotoUri: Uri?
        get() = worksiteImageRepository.newPhotoUri

    val actionDescriptionMessage = MutableStateFlow("")

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

        val pinMarkerSize = Pair(32f, 48f)
        viewModelScope.launch {
            inBoundsPinIcon = drawableResourceBitmapProvider.getIcon(
                com.crisiscleanup.core.common.R.drawable.cc_foreground_pin,
                pinMarkerSize,
            )
            outOfBoundsPinIcon = drawableResourceBitmapProvider.getIcon(
                com.crisiscleanup.core.mapmarker.R.drawable.cc_pin_location_out_of_bounds,
                pinMarkerSize,
            )
            mapMarkerIcon.value = inBoundsPinIcon
        }

        permissionManager.permissionChanges.map {
            if (it == cameraPermissionGranted) {
                caseMediaManager.continueTakePhotoGate.set(true)
                isCameraPermissionGranted = true
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            accountDataRefresher.updateMyOrganization(false)
        }

        viewModelScope.launch(ioDispatcher) {
            organizationRefresher.pullOrganization(incidentIdArg)
        }

        worksiteInteractor.onSelectCase(incidentIdArg, worksiteIdArg)
    }

    val isLoading = dataLoader.isLoading

    private val viewState = dataLoader.viewState

    private val referenceWorksite: Worksite
        get() = viewState.value.asCaseData()?.worksite ?: EmptyWorksite

    private val filesNotes = processWorksiteFilesNotes(
        editableWorksiteProvider.editableWorksite,
        viewState,
    )

    val statusOptions = viewState
        .mapLatest {
            it.asCaseData()?.statusOptions ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val caseData = viewState.map(CaseEditorViewState::asCaseData)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    val otherNotes = editableWorksiteProvider.editableWorksite.flatMapLatest {
        editableWorksiteProvider.otherNotes
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val tabTitles = combine(
        filesNotes,
        otherNotes,
        ::Pair,
    )
        .mapLatest { (fn, on) ->
            val (fileImages, localImages, notes) = fn

            val fileCount = fileImages.size + localImages.size
            val photosTitle = translate("caseForm.photos").let {
                if (fileCount > 0) "$it ($fileCount)" else it
            }

            val noteCount = notes.size + on.size
            val notesTitle = translate("formLabels.notes").let {
                if (noteCount > 0) "$it ($noteCount)" else it
            }

            listOf(
                translate("nav.info"),
                photosTitle,
                notesTitle,
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(3_000),
        )

    val subTitle = editableWorksite.mapLatest {
        if (it.isNew) {
            ""
        } else {
            listOf(it.county, it.state)
                .filter { s -> s.isNotBlank() }
                .joinToString(", ")
        }
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val workTypeProfile = combine(
        viewState,
        editableWorksite,
        organizationLookup,
        ::Triple,
    )
        .filter {
            it.first is CaseEditorViewState.CaseData &&
                !it.second.isNew &&
                it.third.isNotEmpty()
        }
        .filter {
            val (viewModelState, _, orgLookup) = it
            val stateData = viewModelState as CaseEditorViewState.CaseData
            val myOrg = orgLookup[stateData.orgId]
            myOrg != null
        }
        .mapLatest {
            val (viewModelState, worksite, orgLookup) = it

            val stateData = viewModelState as CaseEditorViewState.CaseData

            val isTurnOnRelease = stateData.incident.turnOnRelease
            val myOrgId = stateData.orgId
            val worksiteWorkTypes = worksite.workTypes

            val myOrg = orgLookup[myOrgId]!!

            val requestedTypes = stateData.worksite.workTypeRequests
                .filter(WorkTypeRequest::hasNoResponse)
                .map(WorkTypeRequest::workType)
                .toSet()

            val summaries = worksiteWorkTypes.map { workType ->
                val workTypeLiteral = workType.workTypeLiteral
                val workTypeTranslateKey = "workType.$workTypeLiteral"
                var name = translate(workTypeTranslateKey)
                if (name == workTypeTranslateKey) {
                    name = translate(workTypeLiteral)
                }
                val workTypeLookup = stateData.incident.workTypeLookup
                val workTypeValues = worksite.formData?.let { formData ->
                    formData.asSequence()
                        .filter { formValue -> workTypeLookup[formValue.key] == workTypeLiteral }
                } ?: emptyMap<String, WorksiteFormValue>().entries.asSequence()
                val summaryJobTypes = workTypeValues
                    .filter { formValue -> formValue.value.isBooleanTrue }
                    .map { formValue -> translate("formLabels.${formValue.key}") }
                    .filter { jobName -> jobName != name }
                    .filter(String::isNotBlank)
                    .toList()
                val summaryJobDetails = workTypeValues
                    .filterNot { formValue -> formValue.value.isBooleanTrue }
                    .map { formValue ->
                        val title = translate("formLabels.${formValue.key}")
                        val description = translate(formValue.value.valueString)
                        listOf(title, description).combineTrimText(": ")
                    }
                    .filter(String::isNotBlank)
                    .toList()

                val summary = listOf(
                    summaryJobTypes.combineTrimText(),
                    summaryJobDetails.combineTrimText("\n"),
                    workType.recur?.let { rRuleString ->
                        try {
                            return@let RRule(rRuleString).toHumanReadableText(translator)
                        } catch (e: Exception) {
                            logger.logException(e)
                        }
                        null
                    },
                    workType.nextRecurAt?.let { nextRecurAt ->
                        if (nextRecurAt > Clock.System.now()) {
                            try {
                                val nextDate =
                                    nextRecurDateFormat.format(nextRecurAt.toJavaInstant())
                                return@let translate("shareWorksite.next_recur")
                                    .replace("{date}", nextDate)
                            } catch (e: Exception) {
                                logger.logException(e)
                            }
                        }
                        null
                    },
                )
                    .combineTrimText("\n")
                WorkTypeSummary(
                    workType,
                    name,
                    summary,
                    requestedTypes.contains(workType.workTypeLiteral),
                    isTurnOnRelease && workType.isReleaseEligible,
                    myOrgId,
                    myOrg.affiliateIds.contains(workType.orgClaim),
                )
            }

            val claimedWorkType = summaries.filter { summary -> summary.workType.orgClaim != null }
            val unclaimed = summaries
                .filter { summary -> summary.workType.orgClaim == null }
                .sortedBy { summary -> summary.workType.workTypeLiteral }
            val otherOrgClaimedWorkTypes =
                claimedWorkType.filterNot(WorkTypeSummary::isClaimedByMyOrg)
            val orgClaimedWorkTypes = claimedWorkType
                .filter(WorkTypeSummary::isClaimedByMyOrg)
                .sortedBy { summary -> summary.workType.workTypeLiteral }

            val otherOrgClaimMap = mutableMapOf<Long, MutableList<WorkTypeSummary>>()
            otherOrgClaimedWorkTypes.forEach { summary ->
                val orgId = summary.workType.orgClaim!!
                val otherOrgWorkTypes = otherOrgClaimMap[orgId] ?: mutableListOf()
                otherOrgWorkTypes.add(summary)
                otherOrgClaimMap[orgId] = otherOrgWorkTypes
            }
            val otherOrgClaims = otherOrgClaimMap.map { (orgId, summaries) ->
                val name = orgLookup[orgId]?.name
                if (name == null) {
                    refreshOrganizationLookup()
                }
                OrgClaimWorkType(
                    orgId,
                    name ?: "",
                    summaries.sortedBy { summary -> summary.workType.workTypeLiteral },
                    false,
                )
            }

            val myOrgName = myOrg.name
            val orgClaimed = OrgClaimWorkType(myOrgId, myOrgName, orgClaimedWorkTypes, true)

            val releasable = otherOrgClaimedWorkTypes
                .filter { summary -> summary.isReleasable }
                .sortedBy(WorkTypeSummary::name)
            val requestable = otherOrgClaimedWorkTypes
                .filter { summary -> !(summary.isReleasable || summary.isRequested) }
                .sortedBy(WorkTypeSummary::name)
            WorkTypeProfile(
                myOrgId,
                otherOrgClaims,
                orgClaimed,
                unclaimed,
                releasable,
                requestable,

                orgName = myOrgName,
                caseNumber = worksite.caseNumber,
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    // TODO Delete local image db entries where file no longer exists in cache
    val beforeAfterPhotos = filesNotes.organizeBeforeAfterPhotos()
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(),
        )

    private fun updateHeaderTitle(caseNumber: String = "") {
        headerTitle.value = if (caseNumber.isEmpty()) {
            translate("nav.work_view_case")
        } else {
            "${translate("actions.view")} $caseNumber"
        }
    }

    private fun refreshOrganizationLookup() {
        if (!isOrganizationsRefreshed.getAndSet(true)) {
            viewModelScope.launch(ioDispatcher) {
                incidentsRepository.pullIncidentOrganizations(incidentIdArg, true)
            }
        }
    }

    fun removeFlag(flag: WorksiteFlag) {
        val startingWorksite = referenceWorksite
        startingWorksite.flags?.let { worksiteFlags ->
            val flagsDeleted = worksiteFlags.filterNot { it.id == flag.id }
            if (flagsDeleted.size < worksiteFlags.size) {
                val changedWorksite = startingWorksite.copy(flags = flagsDeleted)
                saveWorksiteChange(startingWorksite, changedWorksite)
            }
        }
    }

    private val viewStateCaseData: CaseEditorViewState.CaseData?
        get() = viewState.value.asCaseData()
    private val organizationId: Long?
        get() = viewStateCaseData?.orgId

    private fun saveWorksiteChange(
        startingWorksite: Worksite,
        changedWorksite: Worksite,
        onSaveAction: () -> Unit = {},
    ) {
        if (startingWorksite.isNew ||
            startingWorksite == changedWorksite
        ) {
            return
        }

        organizationId?.let { orgId ->
            viewModelScope.launch(ioDispatcher) {
                isSavingWorksite.value = true
                try {
                    worksiteChangeRepository.saveWorksiteChange(
                        startingWorksite,
                        changedWorksite,
                        changedWorksite.keyWorkType!!,
                        orgId,
                    )

                    syncPusher.appPushWorksite(worksiteIdArg)

                    onSaveAction()
                } catch (e: Exception) {
                    onSaveFail(e)
                } finally {
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

    fun toggleFavorite() {
        val startingWorksite = referenceWorksite
        val changedWorksite =
            startingWorksite.copy(isAssignedToOrgMember = !startingWorksite.isLocalFavorite)
        saveWorksiteChange(startingWorksite, changedWorksite) {
            val messageTranslateKey = if (changedWorksite.isLocalFavorite) {
                "caseView.member_my_org"
            } else {
                "actions.not_member_of_my_org"
            }
            actionDescriptionMessage.value = translate(messageTranslateKey)
        }
    }

    fun toggleHighPriority() {
        val startingWorksite = referenceWorksite
        val changedWorksite = startingWorksite.toggleHighPriorityFlag()
        saveWorksiteChange(startingWorksite, changedWorksite) {
            val messageTranslateKey = if (changedWorksite.hasHighPriorityFlag) {
                "caseView.high_priority"
            } else {
                "caseView.not_high_priority"
            }
            actionDescriptionMessage.value = translate(messageTranslateKey)
        }
    }

    fun jumpToCaseOnMap() {
        caseData.value?.let {
            val coordinates = it.worksite.coordinates
            editableWorksiteProvider.setEditedLocation(coordinates)
            jumpToCaseOnMapOnBack.value = true
        }
    }

    private fun saveWorkTypeChange(
        startingWorksite: Worksite,
        changedWorksite: Worksite,
    ) {
        var updatedWorksite = changedWorksite

        var workTypes = changedWorksite.workTypes
        if (workTypes.isNotEmpty()) {
            workTypes = workTypes.sortedBy(WorkType::workTypeLiteral)

            var keyWorkType = workTypes.first()
            changedWorksite.keyWorkType?.workTypeLiteral?.let { keyWorkTypeLiteral ->
                workTypes.find { keyWorkTypeLiteral == it.workTypeLiteral }
                    ?.let { matchingWorkType ->
                        keyWorkType = matchingWorkType
                    }
            }

            updatedWorksite = changedWorksite.copy(
                keyWorkType = keyWorkType,
            )
        }

        saveWorksiteChange(startingWorksite, updatedWorksite)
    }

    fun updateWorkType(workType: WorkType, isStatusChange: Boolean) {
        val startingWorksite = referenceWorksite
        val updatedWorkTypes =
            startingWorksite.workTypes
                .filter { it.workType != workType.workType }
                .toMutableList()
                .apply {
                    val changed =
                        if (isStatusChange && workType.orgClaim == null) {
                            workType.copy(orgClaim = organizationId)
                        } else {
                            workType
                        }
                    add(changed)
                }
        val changedWorksite = startingWorksite.copy(workTypes = updatedWorkTypes)
        saveWorkTypeChange(startingWorksite, changedWorksite)
    }

    fun requestWorkType(workType: WorkType) {
        workTypeProfile.value?.let { profile ->
            transferWorkTypeProvider.startTransfer(
                profile.orgId,
                WorkTypeTransferType.Request,
                profile.requestable.associate { summary ->
                    val isSelected = summary.workType.id == workType.id
                    summary.workType to isSelected
                },
                profile.orgName,
                profile.caseNumber,
            )
        }
    }

    fun releaseWorkType(workType: WorkType) {
        workTypeProfile.value?.let { profile ->
            transferWorkTypeProvider.startTransfer(
                profile.orgId,
                WorkTypeTransferType.Release,
                profile.releasable.associate { summary ->
                    val isSelected = summary.workType.id == workType.id
                    summary.workType to isSelected
                },
            )
        }
    }

    fun claimAll() {
        organizationId?.let { orgId ->
            val startingWorksite = referenceWorksite
            val updatedWorkTypes =
                startingWorksite.workTypes
                    .map {
                        if (it.isClaimed) {
                            it
                        } else {
                            it.copy(orgClaim = orgId)
                        }
                    }
            val changedWorksite = startingWorksite.copy(workTypes = updatedWorkTypes)
            saveWorkTypeChange(startingWorksite, changedWorksite)
        }
    }

    fun requestAll() {
        workTypeProfile.value?.let { profile ->
            transferWorkTypeProvider.startTransfer(
                profile.orgId,
                WorkTypeTransferType.Request,
                profile.requestable.associate { summary -> summary.workType to true },
                profile.orgName,
                profile.caseNumber,
            )
        }
    }

    fun releaseAll() {
        workTypeProfile.value?.let { profile ->
            transferWorkTypeProvider.startTransfer(
                profile.orgId,
                WorkTypeTransferType.Release,
                profile.releasable.associate { summary -> summary.workType to true },
            )
        }
    }

    fun saveNote(note: WorksiteNote) {
        if (note.note.isBlank()) {
            return
        }

        val startingWorksite = referenceWorksite
        val notes = mutableListOf(note).apply { addAll(startingWorksite.notes) }
        val changedWorksite = startingWorksite.copy(notes = notes)
        saveWorksiteChange(startingWorksite, changedWorksite)
    }

    fun scheduleSync() {
        if (!isSyncing.value) {
            syncPusher.appPushWorksite(worksiteIdArg)
            syncPusher.scheduleSyncMedia()
        }
    }

    // CaseCameraMediaManager

    override fun takePhoto() = caseMediaManager.takePhoto { showExplainPermissionCamera = true }

    override fun continueTakePhoto() = caseMediaManager.continueTakePhotoGate.getAndSet(false)

    override fun onMediaSelected(uri: Uri, isFileSelected: Boolean) {
        caseMediaManager.onMediaSelected(
            editableWorksite.value.id,
            addImageCategory.literal,
            uri,
            isFileSelected,
        ) { e -> onSaveFail(e, true) }
    }

    override fun onMediaSelected(uris: List<Uri>) {
        uris.forEach {
            onMediaSelected(it, true)
        }
    }

    override fun onDeleteImage(image: CaseImage) {
        caseMediaManager.deleteImage(image.id, image.isNetworkImage)
    }

    // KeyResourceTranslator

    override val translationCount = translator.translationCount

    override fun translate(phraseKey: String) = translate(phraseKey, 0)

    override fun translate(phraseKey: String, fallbackResId: Int) =
        editableWorksiteProvider.translate(phraseKey) ?: translator.translate(
            phraseKey,
            fallbackResId,
        )
}

data class WorkTypeSummary(
    val workType: WorkType,
    val name: String,
    val jobSummary: String,
    val isRequested: Boolean,
    val isReleasable: Boolean,
    val myOrgId: Long,
    val isClaimedByMyOrg: Boolean,
)

data class OrgClaimWorkType(
    val orgId: Long,
    val orgName: String,
    val workTypes: List<WorkTypeSummary>,
    val isMyOrg: Boolean,
)

data class WorkTypeProfile(
    val orgId: Long,
    val otherOrgClaims: List<OrgClaimWorkType>,
    val orgClaims: OrgClaimWorkType,
    val unclaimed: List<WorkTypeSummary>,
    val releasable: List<WorkTypeSummary>,
    val requestable: List<WorkTypeSummary>,
    val releasableCount: Int = releasable.size,
    val requestableCount: Int = requestable.size,

    val orgName: String,
    val caseNumber: String,
)
