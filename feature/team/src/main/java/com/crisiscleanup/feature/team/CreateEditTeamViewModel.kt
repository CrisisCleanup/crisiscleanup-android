package com.crisiscleanup.feature.team

import android.content.ComponentCallbacks2
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppMemoryStats
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.ReplaySubscribed3
import com.crisiscleanup.core.common.event.TrimMemoryEventManager
import com.crisiscleanup.core.common.event.TrimMemoryListener
import com.crisiscleanup.core.common.locationPermissionGranted
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.commoncase.CasesCounter
import com.crisiscleanup.core.commoncase.map.CasesMapBoundsManager
import com.crisiscleanup.core.commoncase.map.CasesMapMarkerManager
import com.crisiscleanup.core.commoncase.map.CasesMapTileLayerManager
import com.crisiscleanup.core.commoncase.map.CasesOverviewMapTileRenderer
import com.crisiscleanup.core.commoncase.map.MapTileRefresher
import com.crisiscleanup.core.data.IncidentRefresher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.LanguageRefresher
import com.crisiscleanup.core.data.OrganizationRefresher
import com.crisiscleanup.core.data.UserRoleRefresher
import com.crisiscleanup.core.data.WorksiteInteractor
import com.crisiscleanup.core.data.di.CasesFilterType
import com.crisiscleanup.core.data.di.CasesFilterTypes
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.EquipmentRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.TeamChangeRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.data.util.IncidentDataPullReporter
import com.crisiscleanup.core.data.util.dataPullProgress
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.WorkTypeChipIconProvider
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.EquipmentData
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.PersonOrganization
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.model.data.zeroDataProgress
import com.crisiscleanup.feature.team.model.TeamEditorStep
import com.crisiscleanup.feature.team.model.stepFromLiteral
import com.crisiscleanup.feature.team.navigation.TeamEditorArgs
import com.crisiscleanup.feature.team.util.NameGenerator
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@HiltViewModel
class CreateEditTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    databaseManagementRepository: AppDataManagementRepository,
    private val accountDataRepository: AccountDataRepository,
    private val organizationsRepository: OrganizationsRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    organizationRefresher: OrganizationRefresher,
    userRoleRefresher: UserRoleRefresher,
    teamsRepository: TeamsRepository,
    private val teamChangeRepository: TeamChangeRepository,
    private val editableTeamProvider: EditableTeamProvider,
    workTypeChipIconProvider: WorkTypeChipIconProvider,
    usersRepository: UsersRepository,
    incidentSelector: IncidentSelector,
    dataPullReporter: IncidentDataPullReporter,
    incidentBoundsProvider: IncidentBoundsProvider,
    private val worksitesRepository: WorksitesRepository,
    @CasesFilterType(CasesFilterTypes.TeamCases)
    mapTileRenderer: CasesOverviewMapTileRenderer,
    @CasesFilterType(CasesFilterTypes.TeamCases)
    tileProvider: TileProvider,
    @CasesFilterType(CasesFilterTypes.TeamCases)
    filterRepository: CasesFilterRepository,
    val mapCaseIconProvider: MapCaseIconProvider,
    worksiteInteractor: WorksiteInteractor,
    equipmentRepository: EquipmentRepository,
    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    languageRefresher: LanguageRefresher,
    private val teamNameGenerator: NameGenerator,
    syncPuller: SyncPuller,
    private val syncPusher: SyncPusher,
    networkMonitor: NetworkMonitor,
    private val translator: KeyResourceTranslator,
    appMemoryStats: AppMemoryStats,
    trimMemoryEventManager: TrimMemoryEventManager,
    appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(),
    EditableTeamDataGuarder,
    KeyTranslator,
    TrimMemoryListener {
    private val teamEditorArgs = TeamEditorArgs(savedStateHandle)
    private val incidentIdArg = teamEditorArgs.incidentId
    private val teamIdArg = teamEditorArgs.teamId
    private val startingEditorStepArg = stepFromLiteral(teamEditorArgs.editorStep)

    private val stepTabOrder = MutableStateFlow(
        listOf(
            TeamEditorStep.Info,
            TeamEditorStep.Members,
            TeamEditorStep.Cases,
            TeamEditorStep.Equipment,
            TeamEditorStep.Review,
        ),
    )

    val stepTabState = stepTabOrder.map { order ->
        val titles = order.mapIndexed { index, step ->
            "${index + 1}. ${translate(step.translateKey)}"
        }
        val startingIndex = order.indexOf(startingEditorStepArg)
            .coerceIn(0, titles.size - 1)
        CreateEditTeamTabState(titles, startingIndex)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = CreateEditTeamTabState(),
            started = ReplaySubscribed3,
        )

    val isNotOnline = networkMonitor.isNotOnline

    val editingTeam = editableTeamProvider.editableTeam

    private val dataLoader: TeamEditorDataLoader

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

    // TODO After team is loaded
    var headerTitle by mutableStateOf("")
        private set
    var headerSubTitle by mutableStateOf("")
        private set

    var editingTeamNotes by mutableStateOf("")
        private set
    var editingTeamName by mutableStateOf("")
        private set
    val editingTeamMembers = MutableStateFlow(emptyList<PersonContact>())
    private val teamMemberIds = editingTeamMembers
        .mapLatest {
            it.map(PersonContact::id).toSet()
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptySet(),
            started = ReplaySubscribed3,
        )
    val memberOptions = editingTeamMembers
        .map { members ->
            members.sortedBy { it.fullName }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = ReplaySubscribed3,
        )

    val teamMemberFilter = MutableStateFlow("")
    private val allMembers = combine(
        accountDataRepository.accountData,
        teamMemberIds,
        ::Pair,
    )
        .flatMapLatest { (accountData, filterIds) ->
            usersRepository
                .streamTeamMembers(incidentIdArg, accountData.org.id)
                .exclude(filterIds)
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = ReplaySubscribed3,
        )

    private val teamMemberQ = teamMemberFilter
        .mapLatest(String::trim)
        .distinctUntilChanged()
        .throttleLatest(150)
    private val filteredMembers = combine(
        teamMemberQ,
        accountDataRepository.accountData,
        teamMemberIds,
        ::Triple,
    )
        .mapLatest { (q, accountData, filterIds) ->
            // TODO Cache results for faster access (search matchingIncidents)
            val matchingMembers = usersRepository
                .getMatchingTeamMembers(q, incidentIdArg, accountData.org.id)
                .exclude(filterIds)

            Pair(q, matchingMembers)
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = Pair("", emptyList()),
            started = ReplaySubscribed3,
        )
    val teamMembersState = teamMemberQ
        .flatMapLatest { q ->
            if (q.isBlank()) {
                allMembers.mapLatest { MemberFilterResult(q, it) }
            } else {
                filteredMembers.mapLatest { results ->
                    if (q.trim() == results.first.trim()) {
                        MemberFilterResult(q, results.second)
                    } else {
                        MemberFilterResult(q, isFiltering = true)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = MemberFilterResult(),
            started = ReplaySubscribed3,
        )

    private val qsm = TeamCasesQueryStateManager(
        incidentSelector,
        filterRepository,
        viewModelScope,
    )
    val isCaseListView = qsm.isListView

    private val incidentWorksitesCount =
        worksitesRepository.streamIncidentWorksitesCount(
            incidentSelector.incidentId,
            useTeamFilters = true,
        )
            .flowOn(ioDispatcher)
            .shareIn(
                scope = viewModelScope,
                replay = 1,
                started = ReplaySubscribed3,
            )

    val isIncidentLoading = incidentsRepository.isLoading

    val dataProgress = dataPullReporter.dataPullProgress
        .stateIn(
            scope = viewModelScope,
            initialValue = zeroDataProgress,
            started = ReplaySubscribed3,
        )

    /**
     * Incident or worksites data are currently saving/caching/loading
     */
    val isLoadingData = combine(
        isIncidentLoading,
        dataProgress,
        worksitesRepository.isDeterminingWorksitesCount,
    ) { b0, progress, b2 -> b0 || progress.isLoadingPrimary || b2 }

    private val mapBoundsManager = CasesMapBoundsManager(
        viewModelScope,
        incidentSelector,
        incidentBoundsProvider,
        ioDispatcher,
        logger,
    )

    private val mapMarkerManager = CasesMapMarkerManager(
        isTeamCasesMap = true,
        worksitesRepository,
        qsm.worksiteQueryState,
        mapBoundsManager,
        worksiteInteractor,
        mapCaseIconProvider,
        appMemoryStats,
        locationProvider,
        viewModelScope,
        ioDispatcher,
    )
    private val worksitesMapMarkers = mapMarkerManager.worksitesMapMarkers

    private val casesCounter = CasesCounter(
        incidentSelector,
        incidentWorksitesCount,
        isLoadingData,
        isMapVisible = qsm.isListView.map(Boolean::not),
        worksitesMapMarkers,
        translator,
        viewModelScope,
        ioDispatcher,
    )

    private val casesMapTileManager = CasesMapTileLayerManager(
        viewModelScope,
        incidentSelector,
        mapBoundsManager,
        logger,
    )

    private val mapTileRefresher = MapTileRefresher(
        mapTileRenderer,
        casesMapTileManager,
    )

    private val isMyLocationEnabled = MutableStateFlow(false)

    val caseMapManager: TeamCaseMapManager = CreateEditTeamCaseMapManager(
        qsm,
        dataProgress,
        isLoadingData,
        mapMarkerManager.isGeneratingWorksiteMarkers,
        worksitesMapMarkers,
        isMyLocationEnabled,
        incidentSelector,
        tileProvider,
        mapTileRenderer,
        mapBoundsManager,
        filterRepository,
        casesCounter,
        permissionManager,
        locationProvider,
        syncPuller,
        coroutineScope = viewModelScope,
        logger,
    )

    private val loadingSelectedMapWorksiteId = MutableStateFlow(EmptyWorksite.id)
    val selectedMapWorksite = MutableStateFlow(EmptyTeamAssignableWorksite)
    val isLoadingMapMarkerWorksite = combine(
        loadingSelectedMapWorksiteId,
        selectedMapWorksite,
        ::Pair,
    )
        .map { (id, assignable) ->
            assignable != EmptyTeamAssignableWorksite &&
                id != assignable.worksite.id
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )
    val isAssigningWorksite = MutableStateFlow(false)
    val assignedWorksites = mutableStateListOf<Worksite>()
    private val assignedWorksiteIds = mutableSetOf<Long>()

    val equipmentOptions = equipmentRepository.streamEquipmentLookup
        .map {
            val equipment = it.values
            equipment.sortedBy { e -> translate(e.nameKey) }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )
    private val editingTeamEquipment = MutableStateFlow<Map<Long, MutableSet<EquipmentData>>>(
        emptyMap(),
    )
    val teamEquipment = editingTeamEquipment
        .mapLatest { equipmentLookup ->
            val teamEquipment = mutableListOf<SinglePersonEquipment>()
            for ((userId, equipment) in equipmentLookup) {
                if (equipment.isNotEmpty()) {
                    editingTeamMembers.value
                        .firstOrNull { member ->
                            member.id == userId
                        }
                        ?.let { person ->
                            val sortedEquipment = equipment.toMutableList().sortedBy { e ->
                                translate(e.nameKey)
                            }
                            teamEquipment.add(
                                SinglePersonEquipment(person, sortedEquipment),
                            )
                        }
                }
            }
            teamEquipment.sortedBy { te -> te.person.fullName }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        trimMemoryEventManager.addListener(this)

        mapTileRenderer.enableTileBoundaries()
        viewModelScope.launch {
            setTileRendererLocation()
        }

        combine(
            incidentWorksitesCount,
            dataPullReporter.incidentDataPullStats,
            filterRepository.casesFiltersLocation,
            ::Triple,
        )
            .debounce(16)
            .throttleLatest(1_000)
            .onEach { mapTileRefresher.refreshTiles(it.first, it.second) }
            .launchIn(viewModelScope)

        permissionManager.permissionChanges
            .map {
                if (it == locationPermissionGranted) {
                    setTileRendererLocation()

                    if (qsm.worksiteQueryState.value.isMapView) {
                        caseMapManager.setMapToMyCoordinates()
                    }
                }
                isMyLocationEnabled.value = permissionManager.hasLocationPermission.value
            }
            .launchIn(viewModelScope)

        dataPullReporter.onIncidentDataPullComplete
            .onEach {
                filterRepository.reapplyFilters()
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            databaseManagementRepository.rebuildFts()
        }

        updateHeaderTitles()

        editableTeamProvider.reset(incidentIdArg)

        dataLoader = TeamEditorDataLoader(
            false,
            incidentIdArg,
            teamIdArg,
            accountDataRepository,
            incidentsRepository,
            incidentRefresher,
            userRoleRefresher,
            teamsRepository,
            teamChangeRepository,
            languageRefresher,
            usersRepository,
            translator,
            editableTeamProvider,
            this,
            workTypeChipIconProvider,
            permissionManager,
            locationProvider,
            viewModelScope,
            ioDispatcher,
            appEnv,
            logger,
        )

        dataLoader.teamStream
            .filterNotNull()
            .onEach {
                updateHeaderTitles(it.team.name)
            }
            .launchIn(viewModelScope)

        dataLoader.viewState
            .filter {
                it.asTeamData()?.isNetworkLoadFinished == true &&
                    isEditableTeamOpen
            }
            .mapLatest {
                editorSetInstant = Clock.System.now()

                it.asTeamData()?.team?.let { team ->
                    with(team) {
                        editingTeamName = name
                        editingTeamNotes = notes
                        editingTeamMembers.value = members
                        synchronized(assignedWorksites) {
                            assignedWorksites.addAll(
                                worksites.filter { worksite ->
                                    !assignedWorksiteIds.contains(worksite.id)
                                },
                            )
                            assignedWorksiteIds.addAll(worksites.map(Worksite::id))
                        }
                        val initialUserEquipment = mutableMapOf<Long, MutableSet<EquipmentData>>()
                        for (equipmentEntry in memberEquipment) {
                            val userId = equipmentEntry.userId
                            if (!initialUserEquipment.contains(userId)) {
                                initialUserEquipment[userId] = mutableSetOf()
                            }
                            initialUserEquipment[userId]!!.add(equipmentEntry.equipmentData)
                        }
                        editingTeamEquipment.value = initialUserEquipment
                    }
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            organizationRefresher.pullOrganizationAndAffiliates()
            organizationRefresher.pullOrganizationUsers()
        }

        snapshotFlow { assignedWorksites.map { it.id }.toSet() }
            .onEach {
                qsm.teamWorksiteIds.value = it
            }
            .launchIn(viewModelScope)
    }

    val isLoading = dataLoader.isLoading

    val isPendingSync = dataLoader.isPendingSync

    val userRoleLookup = dataLoader.userRoleLookup

    val worksiteWorkTypeIconLookup = dataLoader.worksiteWorkTypeIconLookup
    val worksiteDistances = dataLoader.worksiteDistances

    private fun updateHeaderTitles(teamName: String = "") {
        val titleKey = if (teamIdArg == EmptyCleanupTeam.id) {
            "~~Create Team"
        } else {
            "~~Edit Team"
        }
        headerTitle = translate(titleKey)
        headerSubTitle = teamName
    }

    fun onTeamNameChange(name: String) {
        editingTeamName = name
    }

    fun onSuggestTeamName() {
        editingTeamName = teamNameGenerator.generateName()
    }

    fun onTeamNotesChange(notes: String) {
        editingTeamNotes = notes
    }

    fun onUpdateTeamMemberFilter(filter: String) {
        teamMemberFilter.value = filter
    }

    fun onRemoveTeamMember(person: PersonContact) {
        if (teamMemberIds.value.contains(person.id)) {
            editingTeamMembers.value = editingTeamMembers.value.toMutableList().also {
                it.remove(person)
            }
        }
    }

    fun onAddTeamMember(person: PersonContact) {
        if (!teamMemberIds.value.contains(person.id)) {
            editingTeamMembers.value = editingTeamMembers.value.toMutableList().also {
                it.add(person)
            }
        }
    }

    private suspend fun setTileRendererLocation() = caseMapManager.setTileRendererLocation()

    private suspend fun getAssignableWorksite(worksiteId: Long): TeamAssignableWorksite {
        val worksite = worksitesRepository.getWorksite(worksiteId)
        val orgId = accountDataRepository.accountData.first().org.id
        val orgIds = organizationsRepository.getOrganizationAffiliateIds(orgId, true)
        val isAssignable = worksite.isAssignable(orgIds)
        val isAssigned = assignedWorksiteIds.contains(worksiteId)
        return TeamAssignableWorksite(
            worksite,
            isAssignable,
            isAssigned = isAssigned,
        )
    }

    fun onMapCaseMarkerSelect(mapCase: WorksiteMapMark) {
        val worksiteId = mapCase.id

        if (loadingSelectedMapWorksiteId.value == worksiteId) {
            return
        }
        loadingSelectedMapWorksiteId.value = worksiteId

        viewModelScope.launch(ioDispatcher) {
            try {
                selectedMapWorksite.value = getAssignableWorksite(worksiteId)
            } catch (e: Exception) {
                logger.logException(e)
                // TODO Alert
            } finally {
                loadingSelectedMapWorksiteId.compareAndSet(worksiteId, EmptyWorksite.id)
            }
        }
    }

    private fun updateSelectedMapWorksite(worksite: Worksite) {
        val selectedWorksite = selectedMapWorksite.value
        if (selectedWorksite.worksite == worksite) {
            val updatedWorksite = selectedWorksite.copy(
                isAssigned = assignedWorksiteIds.contains(worksite.id),
            )
            selectedMapWorksite.compareAndSet(selectedWorksite, updatedWorksite)

            (caseMapManager as CreateEditTeamCaseMapManager).centerMapOnWorksite(
                worksite,
                qsm.mapZoom.value,
            )
            selectedMapWorksite.value = EmptyTeamAssignableWorksite
        }
    }

    fun onAssignCase(existingWorksite: ExistingWorksiteIdentifier) {
        if (existingWorksite == ExistingWorksiteIdentifierNone ||
            existingWorksite.incidentId != incidentIdArg
        ) {
            return
        }

        if (!isAssigningWorksite.compareAndSet(expect = false, update = true)) {
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val worksiteId = existingWorksite.worksiteId
            try {
                val assignableWorksite = getAssignableWorksite(worksiteId)
                if (assignableWorksite.isAssignable) {
                    synchronized(assignedWorksites) {
                        val isAssigned = assignedWorksiteIds.contains(worksiteId)
                        if (!isAssigned) {
                            assignedWorksiteIds.add(worksiteId)
                            assignedWorksites.add(assignableWorksite.worksite)
                        }
                    }
                    updateSelectedMapWorksite(assignableWorksite.worksite)
                } else {
                    // TODO Alert Case is not assignable
                }
            } finally {
                isAssigningWorksite.value = false
            }
        }
    }

    fun onUnassignCase(worksite: Worksite) {
        if (assignedWorksiteIds.contains(worksite.id)) {
            synchronized(assignedWorksites) {
                assignedWorksites.remove(worksite)
                assignedWorksiteIds.remove(worksite.id)
            }
            updateSelectedMapWorksite(worksite)
        }
    }

    fun clearSelectedMapCase() {
        loadingSelectedMapWorksiteId.value = EmptyWorksite.id
        selectedMapWorksite.value = EmptyTeamAssignableWorksite
    }

    private fun setContentViewType(isListView: Boolean) {
        isCaseListView.value = isListView

        if (!isListView) {
            mapBoundsManager.restoreBounds()
        }
    }

    fun toggleMapListView() {
        setContentViewType(!isCaseListView.value)
    }

    fun saveChanges(
        claimUnclaimed: Boolean,
        backOnSuccess: Boolean = true,
    ) {
        // TODO Require name if blank
    }

    // EditableTeamDataGuarder

    override val isEditableTeamOpen: Boolean
        get() = editorSetInstant?.let { Clock.System.now() - it < editorSetWindow } ?: true

    // KeyTranslator

    override val translationCount = translator.translationCount

    override fun translate(phraseKey: String) = translator.translate(phraseKey) ?: phraseKey

    // TrimMemoryListener

    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                casesMapTileManager.clearTiles()
            }
        }
    }
}

private fun List<PersonOrganization>.exclude(ids: Set<Long>) = filter {
    !ids.contains(it.person.id)
}

private fun Flow<List<PersonOrganization>>.exclude(ids: Set<Long>) = map { it.exclude(ids) }

data class CreateEditTeamTabState(
    val titles: List<String> = emptyList(),
    val startingIndex: Int = 0,
    val casesTabIndex: Int = 2,
)

data class MemberFilterResult(
    val q: String = "",
    val members: List<PersonOrganization> = emptyList(),
    val isFiltering: Boolean = false,
)

data class TeamAssignableWorksite(
    val worksite: Worksite,
    val isAssignable: Boolean,
    val isAssigned: Boolean,
)

val EmptyTeamAssignableWorksite = TeamAssignableWorksite(
    EmptyWorksite,
    isAssignable = false,
    isAssigned = false,
)

data class SinglePersonEquipment(
    val person: PersonContact,
    val equipment: List<EquipmentData>,
)
