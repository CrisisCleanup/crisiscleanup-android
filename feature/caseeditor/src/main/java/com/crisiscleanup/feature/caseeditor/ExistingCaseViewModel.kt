package com.crisiscleanup.feature.caseeditor

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.LocalImageRepository
import com.crisiscleanup.core.data.repository.LocationsRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.NetworkImage
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeRequest
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteLocalImage
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.feature.caseeditor.model.CaseImage
import com.crisiscleanup.feature.caseeditor.model.ImageCategory
import com.crisiscleanup.feature.caseeditor.model.asCaseImage
import com.crisiscleanup.feature.caseeditor.navigation.ExistingCaseArgs
import com.google.android.gms.maps.model.BitmapDescriptor
import com.philjay.RRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class ExistingCaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    private val incidentsRepository: IncidentsRepository,
    organizationsRepository: OrganizationsRepository,
    incidentRefresher: IncidentRefresher,
    locationsRepository: LocationsRepository,
    incidentBoundsProvider: IncidentBoundsProvider,
    worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    private val localImageRepository: LocalImageRepository,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    val transferWorkTypeProvider: TransferWorkTypeProvider,
    private val permissionManager: PermissionManager,
    private val translator: KeyResourceTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    private val resourceProvider: AndroidResourceProvider,
    packageManager: PackageManager,
    private val contentResolver: ContentResolver,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), KeyResourceTranslator {
    private val caseEditorArgs = ExistingCaseArgs(savedStateHandle)
    private val incidentIdArg = caseEditorArgs.incidentId
    val worksiteIdArg = caseEditorArgs.worksiteId

    val headerTitle = MutableStateFlow("")

    private val nextRecurDateFormat = DateTimeFormatter
        .ofPattern("EEE MMMM d yyyy ['at'] h:mm a")
        .withZone(ZoneId.systemDefault())

    private val dataLoader: CaseEditorDataLoader

    private val editOpenedAt = Clock.System.now()

    val mapMarkerIcon = MutableStateFlow<BitmapDescriptor?>(null)
    private var inBoundsPinIcon: BitmapDescriptor? = null
    private var outOfBoundsPinIcon: BitmapDescriptor? = null

    val isSyncing = worksiteChangeRepository.syncingWorksiteIds.mapLatest {
        it.contains(worksiteIdArg)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val isSavingWorksite = MutableStateFlow(false)
    private val isSavingMedia = MutableStateFlow(false)
    val isSaving = combine(
        isSavingWorksite,
        isSavingMedia,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val syncingWorksiteImage = localImageRepository.syncingWorksiteImage
        .stateIn(
            scope = viewModelScope,
            initialValue = 0L,
            started = SharingStarted.WhileSubscribed(),
        )

    private var isOrganizationsRefreshed = AtomicBoolean(false)
    private val organizationLookup = organizationsRepository.organizationLookup
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(),
        )

    val editableWorksite = editableWorksiteProvider.editableWorksite

    private val previousNoteCount = AtomicInteger(0)

    var addImageCategory by mutableStateOf(ImageCategory.Before)

    val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    var showExplainPermissionCamera by mutableStateOf(false)

    val capturePhotoUri: Uri?
        @SuppressLint("SimpleDateFormat")
        get() {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = "CC_${timeStamp}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/CrisisCleanup")
            }
            return contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )
        }

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
    }

    val isLoading = dataLoader.isLoading

    private val uiState = dataLoader.uiState

    private val referenceWorksite: Worksite
        get() = uiState.value.asCaseData()?.worksite ?: EmptyWorksite

    val tabTitles = editableWorksite.mapLatest { worksite ->
        val files = worksite.files
        val photosTitle = translate("caseForm.photos").let {
            if (files.isNotEmpty()) "$it (${files.size})" else it
        }
        val notes = worksite.notes
        val notesTitle = translate("phoneDashboard.notes").let {
            if (notes.isNotEmpty()) "$it (${notes.size})" else it
        }
        listOf(
            resourceProvider.getString(R.string.info),
            photosTitle,
            notesTitle,
        )
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(3_000)
        )


    val statusOptions = uiState
        .mapLatest {
            it.asCaseData()?.statusOptions ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val caseData = uiState.map { it.asCaseData() }
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    val subTitle = editableWorksite.mapLatest {
        if (it.isNew) ""
        else listOf(it.county, it.state)
            .filter { s -> s.isNotBlank() }
            .joinToString(", ")
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val workTypeProfile = combine(
        uiState,
        editableWorksite,
        organizationLookup,
        ::Triple,
    )
        .filter {
            it.first is CaseEditorUiState.CaseData &&
                    !it.second.isNew &&
                    it.third.isNotEmpty()
        }
        .filter {
            val (viewModelState, _, orgLookup) = it
            val stateData = viewModelState as CaseEditorUiState.CaseData
            val myOrg = orgLookup[stateData.orgId]
            myOrg != null
        }
        .mapLatest {
            val (viewModelState, worksite, orgLookup) = it

            val stateData = viewModelState as CaseEditorUiState.CaseData

            val isTurnOnRelease = stateData.incident.turnOnRelease
            val myOrgId = stateData.orgId
            val worksiteWorkTypes = worksite.workTypes

            val myOrg = orgLookup[myOrgId]!!

            val requestedTypes = stateData.worksite.workTypeRequests
                .map(WorkTypeRequest::workType)
                .toSet()

            val summaries = worksiteWorkTypes.map { workType ->
                val workTypeLiteral = workType.workTypeLiteral
                var name = translate(workTypeLiteral)
                if (name == workTypeLiteral) {
                    name = translate("workType.$workTypeLiteral")
                }
                val workTypeLookup = stateData.incident.workTypeLookup
                val summaryJobTypes = worksite.formData
                    ?.filter { formValue -> workTypeLookup[formValue.key] == workTypeLiteral }
                    ?.filter { formValue -> formValue.value.isBooleanTrue }
                    ?.map { formValue -> translate(formValue.key) }
                    ?.filter { jobName -> jobName != name }
                    ?.filter(String::isNotBlank)
                    ?: emptyList()
                val summary = listOf(
                    summaryJobTypes.combineTrimText(),
                    workType.recur?.let { rRuleString ->
                        try {
                            return@let RRule(rRuleString).toHumanReadableText(resourceProvider)
                        } catch (e: Exception) {
                            logger.logException(e)
                        }
                        null
                    },
                    workType.nextRecurAt?.let { nextRecurAt ->
                        try {
                            val nextDate = nextRecurDateFormat.format(nextRecurAt.toJavaInstant())
                            return@let resourceProvider.getString(R.string.next_recur, nextDate)
                        } catch (e: Exception) {
                            logger.logException(e)
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
            val unclaimed = summaries.filter { summary -> summary.workType.orgClaim == null }
            val otherOrgClaimedWorkTypes =
                claimedWorkType.filterNot(WorkTypeSummary::isClaimedByMyOrg)
            val orgClaimedWorkTypes = claimedWorkType.filter(WorkTypeSummary::isClaimedByMyOrg)

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
                OrgClaimWorkType(orgId, name ?: "", summaries, false)
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

    val beforeAfterPhotos = combine(
        editableWorksiteProvider.editableWorksite,
        uiState,
        ::Pair,
    )
        .filter { (_, state) -> state is CaseEditorUiState.CaseData }
        .mapLatest { (worksite, state) ->
            val localImages = (state as CaseEditorUiState.CaseData).localWorksite
                ?.localImages
                ?.map(WorksiteLocalImage::asCaseImage)
                ?: emptyList()
            val fileImages = worksite.files.map(NetworkImage::asCaseImage)
            val beforeImages = localImages.filterNot(CaseImage::isAfter).toMutableList()
                .apply { addAll(fileImages.filterNot(CaseImage::isAfter)) }
            val afterImages = localImages.filter(CaseImage::isAfter).toMutableList()
                .apply { addAll(fileImages.filter(CaseImage::isAfter)) }
            mapOf(
                ImageCategory.Before to beforeImages,
                ImageCategory.After to afterImages,
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(),
        )

    private fun updateHeaderTitle(caseNumber: String = "") {
        headerTitle.value = if (caseNumber.isEmpty()) translate("nav.work_view_case")
        else resourceProvider.getString(R.string.view_case_number, caseNumber)
    }

    private fun refreshOrganizationLookup() {
        if (!isOrganizationsRefreshed.getAndSet(true)) {
            viewModelScope.launch(ioDispatcher) {
                incidentsRepository.pullIncidentOrganizations(incidentIdArg, true)
            }
        }
    }

    // TODO Queue and debounce saves. Save off view model thread in case is long running.
    //      How to keep worksite state synced?

    private val uiStateCaseData: CaseEditorUiState.CaseData?
        get() = uiState.value.asCaseData()
    private val organizationId: Long?
        get() = uiStateCaseData?.orgId

    private fun saveWorksiteChange(
        startingWorksite: Worksite,
        changedWorksite: Worksite,
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

                    externalScope.launch {
                        syncPusher.appPushWorksite(worksiteIdArg)
                    }
                } catch (e: Exception) {
                    logger.logException(e)

                    // TODO Show dialog save failed. Try again. If still fails seek help.
                } finally {
                    isSavingWorksite.value = false
                }
            }
        }
    }

    fun takeNoteAdded(): Boolean {
        val noteCount = referenceWorksite.notes.size
        return previousNoteCount.getAndSet(noteCount) + 1 == noteCount
    }

    fun toggleFavorite() {
        val startingWorksite = referenceWorksite
        val changedWorksite =
            startingWorksite.copy(isAssignedToOrgMember = !startingWorksite.isLocalFavorite)
        saveWorksiteChange(startingWorksite, changedWorksite)
    }

    fun toggleHighPriority() {
        val startingWorksite = referenceWorksite
        val changedWorksite = startingWorksite.toggleHighPriorityFlag()
        saveWorksiteChange(startingWorksite, changedWorksite)
    }

    fun updateWorkType(workType: WorkType) {
        val startingWorksite = referenceWorksite
        val updatedWorkTypes =
            startingWorksite.workTypes
                .filter { it.workType != workType.workType }
                .toMutableList()
                .apply { add(workType) }
        val changedWorksite = startingWorksite.copy(workTypes = updatedWorkTypes)
        saveWorksiteChange(startingWorksite, changedWorksite)
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
                        if (it.isClaimed) it
                        else it.copy(orgClaim = orgId)
                    }
            val changedWorksite = startingWorksite.copy(workTypes = updatedWorkTypes)
            saveWorksiteChange(startingWorksite, changedWorksite)
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

    fun takePhoto(): Boolean {
        when (permissionManager.requestCameraPermission()) {
            PermissionStatus.Granted -> {
                return true
            }

            PermissionStatus.ShowRationale -> {
                showExplainPermissionCamera = true
            }

            PermissionStatus.Requesting,
            PermissionStatus.Denied,
            PermissionStatus.Undefined -> {
                // Ignore these statuses as they're not important
            }
        }
        return false
    }

    fun onMediaSelected(uri: Uri) {
        isSavingMedia.value = true
        viewModelScope.launch(ioDispatcher) {
            var displayName = ""

            val displayNameColumn = MediaStore.MediaColumns.DISPLAY_NAME
            val projection = arrayOf(
                displayNameColumn,
            )
            contentResolver.query(uri, projection, Bundle.EMPTY, null)?.let {
                it.use { cursor ->
                    with(cursor) {
                        if (moveToFirst()) {
                            displayName = getString(getColumnIndexOrThrow(displayNameColumn))
                        }
                    }
                }
            }

            if (displayName.isNotBlank()) {
                try {
                    localImageRepository.save(
                        WorksiteLocalImage(
                            0,
                            editableWorksite.value.id,
                            documentId = displayName,
                            uri = uri.toString(),
                            tag = addImageCategory.literal,
                        )
                    )

                    syncPusher.scheduleSyncMedia()
                } catch (e: Exception) {
                    // TODO Show error message
                    logger.logException(e)
                } finally {
                    isSavingMedia.value = false
                }
            }
        }
    }

    // KeyResourceTranslator

    override val translationCount = translator.translationCount

    override fun translate(phraseKey: String) = translate(phraseKey, 0)

    override fun translate(phraseKey: String, fallbackResId: Int) =
        editableWorksiteProvider.translate(phraseKey) ?: translator.translate(
            phraseKey,
            fallbackResId
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