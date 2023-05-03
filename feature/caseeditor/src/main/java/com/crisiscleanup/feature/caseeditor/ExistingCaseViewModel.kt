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
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
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
import javax.inject.Inject

@HiltViewModel
class ExistingCaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    private val incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    locationsRepository: LocationsRepository,
    worksitesRepository: WorksitesRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    private val translator: KeyTranslator,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    private val resourceProvider: AndroidResourceProvider,
    drawableResourceBitmapProvider: DrawableResourceBitmapProvider,
    @Logger(CrisisCleanupLoggers.Worksites) logger: AppLogger,
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

    val isSavingWorksite = MutableStateFlow(false)

    private var isOrganizationsRefreshed = AtomicBoolean(false)
    private val organizationLookup = incidentsRepository.organizationNameLookup
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(),
        )

    val worksite = MutableStateFlow(EmptyWorksite)

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

        dataLoader.worksiteStream
            .debounce { 100 }
            .filter { it != null }
            .onEach { worksite.value = it!!.worksite }
            .launchIn(viewModelScope)
    }

    val statusOptions = dataLoader.uiState
        .mapLatest {
            (it as? CaseEditorUiState.WorksiteData)?.statusOptions ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val subTitle = worksite.mapLatest {
        if (it == EmptyWorksite) ""
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
        worksite,
        organizationLookup,
        ::Triple,
    )
        .filter {
            it.first is CaseEditorUiState.WorksiteData &&
                    it.second != EmptyWorksite &&
                    it.third.isNotEmpty()
        }
        .mapLatest {
            val stateData = it.first as CaseEditorUiState.WorksiteData
            val myOrgId = stateData.orgId
            val worksiteWorkTypes = it.second.workTypes

            var isMyOrgClaim = false
            var myOrgName = ""
            val otherOrgClaims = mutableListOf<String>()

            val orgClaims = worksiteWorkTypes.mapNotNull(WorkType::orgClaim).toSet()
            val orgLookup = it.third
            orgClaims.forEach { orgId ->
                val name = orgLookup[orgId]
                if (name == null) {
                    refreshOrganizationLookup()
                } else {
                    if (orgId == myOrgId) {
                        isMyOrgClaim = true
                        myOrgName = name
                    } else {
                        otherOrgClaims.add(name)
                    }
                }
            }
            val workTypeOrgClaims =
                WorkTypeOrgClaims(isMyOrgClaim, myOrgName, otherOrgClaims.sorted())

            val summaries = worksiteWorkTypes.map { workType ->
                val workTypeLiteral = workType.workTypeLiteral
                val workTypeLookup = stateData.incident.workTypeLookup
                val summaryJobTypes = it.second.formData
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
                    myOrgId,
                )
            }

            val unclaimedCount =
                worksiteWorkTypes.filter { workType -> workType.orgClaim == null }.size
            val myOrgClaimedCount =
                worksiteWorkTypes.filter { workType -> workType.orgClaim == myOrgId }.size
            val requestCount =
                worksiteWorkTypes.filter { workType -> workType.orgClaim != null }.size - myOrgClaimedCount

            WorkTypeProfile(
                workTypeOrgClaims,
                summaries,
                unclaimedCount,
                myOrgClaimedCount,
                requestCount,
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
        headerTitle.value = if (caseNumber.isEmpty()) resourceProvider.getString(R.string.view_case)
        else resourceProvider.getString(R.string.view_case_number, caseNumber)
    }

    // TODO Queue and debounce saves. Save off view model thread in case is long running.
    //      How to keep worksite state synced?

    private fun refreshOrganizationLookup() {
        if (!isOrganizationsRefreshed.getAndSet(true)) {
            viewModelScope.launch {
                incidentsRepository.pullIncidentOrganizations(incidentIdArg, true)
            }
        }
    }

    fun toggleFavorite() {

    }

    fun toggleHighPriority() {

    }

    fun updateWorkType(workType: WorkType) {

    }

    fun requestWorkType(workType: WorkType) {

    }

    fun claimAll() {

    }

    fun requestAll() {

    }

    fun releaseAll() {

    }
}

data class WorkTypeOrgClaims(
    val isMyOrgClaim: Boolean,
    val myOrgName: String,
    val otherOrgClaims: List<String>,
)

data class WorkTypeSummary(
    val workType: WorkType,
    val name: String,
    val jobSummary: String,
    val myOrgId: Long,
) {
    val isClaimedByMyOrg = workType.orgClaim == myOrgId
}

data class WorkTypeProfile(
    val orgClaims: WorkTypeOrgClaims,
    val summaries: List<WorkTypeSummary>,
    val unclaimedCount: Int,
    val myOrgClaimedCount: Int,
    val requestCount: Int,
)