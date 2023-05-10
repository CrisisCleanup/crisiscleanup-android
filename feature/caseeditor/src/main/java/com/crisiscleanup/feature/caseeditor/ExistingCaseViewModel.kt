package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.mapmarker.DrawableResourceBitmapProvider
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeRequest
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.navigation.ExistingCaseArgs
import com.google.android.gms.maps.model.BitmapDescriptor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@HiltViewModel
class ExistingCaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    private val incidentsRepository: IncidentsRepository,
    organizationsRepository: OrganizationsRepository,
    incidentRefresher: IncidentRefresher,
    locationsRepository: LocationsRepository,
    worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    val transferWorkTypeProvider: TransferWorkTypeProvider,
    private val translator: KeyTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    private val resourceProvider: AndroidResourceProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val caseEditorArgs = ExistingCaseArgs(savedStateHandle)
    private val incidentIdArg = caseEditorArgs.incidentId
    val worksiteIdArg = caseEditorArgs.worksiteId

    val headerTitle = MutableStateFlow("")

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

    val isSavingWorksite = MutableStateFlow(false)

    private var isOrganizationsRefreshed = AtomicBoolean(false)
    private val organizationLookup = organizationsRepository.organizationLookup
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(),
        )

    val editableWorksite = editableWorksiteProvider.editableWorksite

    // The first worksite loaded in the session
    private val worksiteIn = AtomicReference<Worksite>()

    init {
        updateHeaderTitle()

        editableWorksiteProvider.reset(incidentIdArg)

        editableWorksite.onEach {
            if (!it.isNew) {
                worksiteIn.compareAndSet(null, it)
            }
        }
            .launchIn(viewModelScope)

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

    val statusOptions = dataLoader.uiState
        .mapLatest {
            (it as? CaseEditorUiState.WorksiteData)?.statusOptions ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val worksiteData = dataLoader.uiState.map { it as? CaseEditorUiState.WorksiteData }
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
        dataLoader.uiState,
        editableWorksite,
        organizationLookup,
        ::Triple,
    )
        .filter {
            it.first is CaseEditorUiState.WorksiteData &&
                    !it.second.isNew &&
                    it.third.isNotEmpty()
        }
        .filter {
            val (uiState, _, orgLookup) = it
            val stateData = uiState as CaseEditorUiState.WorksiteData
            val myOrg = orgLookup[stateData.orgId]
            myOrg != null
        }
        .mapLatest {
            val (uiState, worksite, orgLookup) = it

            val stateData = uiState as CaseEditorUiState.WorksiteData

            val isTurnOnRelease = stateData.incident.turnOnRelease
            val myOrgId = stateData.orgId
            val worksiteWorkTypes = worksite.workTypes

            val myOrg = orgLookup[myOrgId]!!

            val requestedTypes = stateData.worksite.workTypeRequests
                .map(WorkTypeRequest::workType)
                .toSet()

            val summaries = worksiteWorkTypes.map { workType ->
                val workTypeLiteral = workType.workTypeLiteral
                val workTypeLookup = stateData.incident.workTypeLookup
                val summaryJobTypes = worksite.formData
                    ?.filter { formValue -> workTypeLookup[formValue.key] == workTypeLiteral }
                    ?.filter { formValue -> formValue.value.isBooleanTrue }
                    ?.map { formValue -> translate(formValue.key) }
                    ?.filter(String::isNotBlank)
                    ?: emptyList()
                var name = translate(workTypeLiteral)
                if (name == workTypeLiteral) {
                    name = translate("workType.$workTypeLiteral")
                }
                WorkTypeSummary(
                    workType,
                    name,
                    summaryJobTypes.combineTrimText(),
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

    fun translate(key: String, fallback: String? = null) = translator.translate(key)
        ?: (editableWorksiteProvider.translate(key) ?: (fallback ?: key))

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

    private val uiStateWorksiteData: CaseEditorUiState.WorksiteData?
        get() = dataLoader.uiState.value as? CaseEditorUiState.WorksiteData
    private val organizationId: Long?
        get() = uiStateWorksiteData?.orgId

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

    fun toggleFavorite() {
        val startingWorksite = editableWorksite.value
        val changedWorksite =
            startingWorksite.copy(isAssignedToOrgMember = !startingWorksite.isLocalFavorite)
        saveWorksiteChange(startingWorksite, changedWorksite)
    }

    fun toggleHighPriority() {
        val startingWorksite = editableWorksite.value
        val changedWorksite = startingWorksite.toggleHighPriorityFlag()
        saveWorksiteChange(startingWorksite, changedWorksite)
    }

    fun updateWorkType(workType: WorkType) {
        val startingWorksite = editableWorksite.value
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
            val startingWorksite = editableWorksite.value
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