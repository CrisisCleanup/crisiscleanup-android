package com.crisiscleanup.feature.team

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.ReplaySubscribed3
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentRefresher
import com.crisiscleanup.core.data.LanguageRefresher
import com.crisiscleanup.core.data.OrganizationRefresher
import com.crisiscleanup.core.data.UserRoleRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.TeamChangeRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.mapmarker.WorkTypeChipIconProvider
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.PersonOrganization
import com.crisiscleanup.feature.team.model.TeamEditorStep
import com.crisiscleanup.feature.team.model.stepFromLiteral
import com.crisiscleanup.feature.team.navigation.TeamEditorArgs
import com.crisiscleanup.feature.team.util.NameGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class CreateEditTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    databaseManagementRepository: AppDataManagementRepository,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    organizationRefresher: OrganizationRefresher,
    userRoleRefresher: UserRoleRefresher,
    teamsRepository: TeamsRepository,
    private val teamChangeRepository: TeamChangeRepository,
    private val editableTeamProvider: EditableTeamProvider,
    workTypeChipIconProvider: WorkTypeChipIconProvider,
    usersRepository: UsersRepository,
    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    languageRefresher: LanguageRefresher,
    private val teamNameGenerator: NameGenerator,
    private val syncPusher: SyncPusher,
    private val translator: KeyTranslator,
    appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), EditableTeamDataGuarder, KeyTranslator {
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

    init {
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
                    }
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            organizationRefresher.pullOrganizationAndAffiliates()
        }
    }

    val isLoading = dataLoader.isLoading

    val isPendingSync = dataLoader.isPendingSync

    @OptIn(FlowPreview::class)
    val profilePictureLookup = combine(
        dataLoader.profilePictureLookup,
        allMembers
            .debounce(1.seconds.inWholeMilliseconds)
            .mapLatest { it.map(PersonOrganization::person) },
        ::Pair,
    )
        .mapLatest { (avatarLookup, allMembers) ->
            val allLookup = allMembers.associateBy { it.id }
            if (allLookup.isEmpty()) {
                avatarLookup
            } else {
                val mergedLookup = buildProfilePicLookup(allLookup).toMutableMap()
                for (entry in avatarLookup) {
                    mergedLookup[entry.key] = entry.value
                }
                mergedLookup
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = ReplaySubscribed3,
        )

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
}

private fun List<PersonOrganization>.exclude(ids: Set<Long>) = filter {
    !ids.contains(it.person.id)
}

private fun Flow<List<PersonOrganization>>.exclude(ids: Set<Long>) = map { it.exclude(ids) }

data class CreateEditTeamTabState(
    val titles: List<String> = emptyList(),
    val startingIndex: Int = 0,
)

data class MemberFilterResult(
    val q: String = "",
    val members: List<PersonOrganization> = emptyList(),
    val isFiltering: Boolean = false,
)
