package com.crisiscleanup.feature.team

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appcomponent.AppTopBarDataProvider
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.ReplaySubscribed3
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.EquipmentRepository
import com.crisiscleanup.core.data.repository.IncidentTeams
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.ExistingUserCodeInviteAccept
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.PersonContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@HiltViewModel
class TeamsViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    val incidentSelector: IncidentSelector,
    private val accountDataRepository: AccountDataRepository,
    appPreferencesRepository: LocalAppPreferencesRepository,
    private val teamsRepository: TeamsRepository,
    private val usersRepository: UsersRepository,
    private val equipmentRepository: EquipmentRepository,
    private val externalEventBus: ExternalEventBus,
    private val syncPuller: SyncPuller,
    private val translator: KeyResourceTranslator,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
) : ViewModel() {
    val appTopBarDataProvider = AppTopBarDataProvider(
        "nav.organization_teams",
        incidentsRepository,
        worksitesRepository,
        incidentSelector,
        translator,
        accountDataRepository,
        appPreferencesRepository,
        viewModelScope,
    )
    val incidentsData = appTopBarDataProvider.incidentsData
    val loadSelectIncidents = appTopBarDataProvider.loadSelectIncidents

    private val incidentIdStream = incidentSelector.incidentId
    private val additionalUserProfileLookup = MutableStateFlow<Map<Long, PersonContact>>(emptyMap())
    val viewState = incidentIdStream
        .flatMapLatest { incidentId ->
            if (incidentId == EmptyIncident.id) {
                return@flatMapLatest flowOf(TeamsViewState.Loading)
            }

            fun parseProfiles(
                teams: List<CleanupTeam>,
            ): Pair<
                Map<Long, PersonContact>,
                Collection<Long>,
                > {
                val profileLookup = teams.flatMap(CleanupTeam::members)
                    .filter { it.profilePictureUri.isNotBlank() }
                    .associateBy(PersonContact::id)
                val missingProfileUserIds = teams.flatMap(CleanupTeam::memberIds)
                    .filter { !profileLookup.contains(it) }
                return Pair(profileLookup, missingProfileUserIds)
            }

            teamsRepository.streamIncidentTeams(incidentId).mapLatest { teams ->
                val myProfiles = parseProfiles(teams.myTeams)
                val otherProfiles = parseProfiles(teams.otherTeams)
                val profileLookup = myProfiles.first.toMutableMap().also { lookup ->
                    lookup.putAll(otherProfiles.first)
                }
                val missingProfileUserIds = myProfiles.second.toMutableSet().also { ids ->
                    ids.addAll(otherProfiles.second)
                }
                TeamsViewState.Success(
                    incidentId,
                    teams,
                    profileLookup,
                    missingProfileUserIds,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = TeamsViewState.Loading,
            started = ReplaySubscribed3,
        )

    val profilePictureLookup = combine(
        viewState,
        additionalUserProfileLookup,
        ::Pair,
    )
        .filter { (state, _) ->
            state is TeamsViewState.Success
        }
        .debounce(1.seconds)
        .mapLatest { (state, additional) ->
            val profileLookup = (state as TeamsViewState.Success).profileLookup
            buildProfilePicLookup(profileLookup, additional)
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = ReplaySubscribed3,
        )

    val isLoading = viewState.map { it == TeamsViewState.Loading }

    val teamFilter = MutableStateFlow("")
    private val teamFilterQ = teamFilter
        .mapLatest(String::trim)
        .distinctUntilChanged()
        .throttleLatest(150)
    val filteredOtherTeams = combine(
        incidentIdStream,
        teamFilterQ,
        viewState,
        ::Triple,
    )
        .flatMapLatest { (incidentId, q, state) ->
            if (q.isBlank()) {
                val otherTeams =
                    (state as? TeamsViewState.Success)?.teams?.otherTeams ?: emptyList()
                flowOf(otherTeams)
            } else {
                teamsRepository.streamMatchingOtherTeams(q, incidentId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = ReplaySubscribed3,
        )

    private var joinTeamInvite = UserPersistentInvite(0, "")
    var isJoiningTeam = MutableStateFlow(false)
    var joinTeamMessage by mutableStateOf("")

    init {
        viewState
            .debounce(1.seconds)
            .onEach { state ->
                (state as? TeamsViewState.Success)?.missingProfileUserIds?.let { ids ->
                    if (ids.isNotEmpty()) {
                        val userProfiles = usersRepository.getUserProfiles(ids)
                        additionalUserProfileLookup.value =
                            userProfiles.associateBy(PersonContact::id)
                    }
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        incidentIdStream.filter { it != EmptyIncident.id }
            .debounce(0.2.seconds)
            .distinctUntilChanged()
            .onEach {
                if (it != EmptyIncident.id) {
                    teamsRepository.syncTeams(it)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            equipmentRepository.saveEquipment()
        }

        externalEventBus.teamPersistentInvites
            .distinctUntilChanged()
            .filter { it.isValidInvite }
            .onEach {
                joinTeamInvite = it
                // Do not clear team invite immediately as it interferes with navigation
                acceptTeamInvite(joinTeamInvite)
            }
            .launchIn(viewModelScope)
    }

    private fun acceptTeamInvite(invite: UserPersistentInvite) =
        viewModelScope.launch(ioDispatcher) {
            if (invite.isValidInvite && !isJoiningTeam.value) {
                isJoiningTeam.value = true

                joinTeamMessage = ""
                try {
                    val emailAddress = accountDataRepository.accountData.first().emailAddress
                    val joinResult = teamsRepository.acceptPersistentInvitation(
                        ExistingUserCodeInviteAccept(
                            emailAddress = emailAddress,
                            invitationCode = invite.inviteToken,
                        ),
                    )

                    if (joinResult == JoinOrgResult.Success) {
                        joinTeamMessage = translator("~~Joined a new team")

                        teamsRepository.getInvitationInfo(invite)?.teamId?.let { networkTeamId ->
                            teamsRepository.syncNetworkTeam(networkTeamId)
                        }
                    } else {
                        var errorMessageTranslateKey = "~~Failed to join team"
                        when (joinResult) {
                            JoinOrgResult.Redundant ->
                                errorMessageTranslateKey = "~~Already a part of the team"

                            else -> {
                                val inviteInfo = teamsRepository.getInvitationInfo(invite)
                                if (inviteInfo?.isExpiredInvite == true) {
                                    errorMessageTranslateKey =
                                        "persistentInvitations.invite_expired_try_again"
                                }
                            }
                        }
                        joinTeamMessage = translator(errorMessageTranslateKey)
                    }

                    // TODO Find a more elegant means to not interfere with navigation
                    Thread.sleep(2.seconds.inWholeMilliseconds)
                } catch (e: Exception) {
                    logger.logException(e)
                    joinTeamMessage = translator("~~Failed to join team. Try again later.")
                } finally {
                    externalEventBus.clearTeamPersistentInvite(invite)
                    isJoiningTeam.value = false
                }
            }
        }

    suspend fun refreshIncidentsAsync() {
        syncPuller.pullIncidents()
    }

    suspend fun refreshTeams() = withContext(ioDispatcher) {
        equipmentRepository.saveEquipment(true)
        teamsRepository.syncTeams(incidentIdStream.value)

        (viewState.value as? TeamsViewState.Success)?.let { state ->
            val refreshUserIds = state.profileLookup.values
                .filter { it.profilePictureUri.isNotBlank() }
                .map { it.id }
            usersRepository.getUserProfiles(refreshUserIds, true)
        }
    }

    fun onUpdateTeamFilter(q: String) {
        teamFilter.value = q
    }
}

private fun buildProfilePicLookup(
    profileLookup: Map<Long, PersonContact>,
    additionalLookup: Map<Long, PersonContact> = emptyMap(),
): Map<Long, String> {
    val lookup = mutableMapOf<Long, String>()
    for (entry in profileLookup) {
        lookup[entry.key] = entry.value.avatarUrl
    }
    for (entry in additionalLookup) {
        lookup[entry.key] = entry.value.avatarUrl
    }
    return lookup
}

sealed interface TeamsViewState {
    data object Loading : TeamsViewState
    data class Success(
        val incidentId: Long,
        val teams: IncidentTeams,
        val profileLookup: Map<Long, PersonContact>,
        val missingProfileUserIds: Set<Long>,
    ) : TeamsViewState
}
