package com.crisiscleanup.feature.team

import android.graphics.Color
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.ReplaySubscribed3
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.TagLogger
import com.crisiscleanup.core.data.IncidentRefresher
import com.crisiscleanup.core.data.LanguageRefresher
import com.crisiscleanup.core.data.UserRoleRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.TeamChangeRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.LocalTeam
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.UserRole
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

internal class TeamEditorDataLoader(
    private val isCreateTeam: Boolean,
    incidentIdIn: Long,
    teamIdIn: Long?,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    userRoleRefresher: UserRoleRefresher,
    teamsRepository: TeamsRepository,
    teamChangeRepository: TeamChangeRepository,
    languageRefresher: LanguageRefresher,
    usersRepository: UsersRepository,
    translator: KeyTranslator,
    private val editableTeamProvider: EditableTeamProvider,
    coroutineScope: CoroutineScope,
    coroutineDispatcher: CoroutineDispatcher,
    appEnv: AppEnv,
    private val logger: AppLogger,
    private val debugTag: String = "",
) {
    private val logDebug = appEnv.isDebuggable && debugTag.isNotBlank()

    private val dataLoadCountStream = MutableStateFlow(0)
    private val isRefreshingIncident = MutableStateFlow(false)
    private val isRefreshingTeam = MutableStateFlow(false)

    val isLoading = combine(
        isRefreshingIncident,
        isRefreshingTeam,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            scope = coroutineScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val organizationStream = accountDataRepository.accountData
        .mapLatest { it.org }
        .distinctUntilChanged()
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3.seconds.inWholeMilliseconds),
        )

    private val incidentStream = incidentsRepository.streamIncident(incidentIdIn)

    private val teamIdStream = MutableStateFlow(teamIdIn)

    val teamStream = teamIdStream
        .flatMapLatest { teamId ->
            if (teamId == null || teamId <= 0) {
                flowOf(null)
            } else {
                teamsRepository.streamLocalTeam(teamId)
            }
        }
        .distinctUntilChanged()
        .flowOn(coroutineDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3.seconds.inWholeMilliseconds),
        )

    private val isInitiallySynced = AtomicBoolean(false)
    private val isTeamPulled = MutableStateFlow(false)

    private val viewStateInternal = combine(
        dataLoadCountStream,
        organizationStream,
        incidentStream,
        teamStream,
        isTeamPulled,
    ) {
            dataLoadCount, organization,
            incident, team, isPulled,
        ->
        Pair(
            Pair(dataLoadCount, organization),
            Triple(incident, team, isPulled),
        )
    }
        .filter { (first, second) ->
            val (_, organization) = first
            val (incident, _, _) = second
            organization != null && incident != null
        }
        .mapLatest { (first, second) ->
            val (_, organization) = first
            val (incident, localTeam, isPulled) = second

            organization!!
            incident!!

            val teamId = teamIdStream.first() ?: -1

            if (organization.id <= 0) {
                logger.logException(Exception("Organization $organization is not set when editing team $teamId"))
                return@mapLatest TeamEditorViewState.Error(
                    errorMessage = translator.translate("info.organization_issue_log_out")
                        ?: "info.organization_issue_log_out",
                )
            }

            val loadedTeam = localTeam?.team
            val teamState = loadedTeam ?: EmptyCleanupTeam.copy(
                incidentId = incidentIdIn,
                colorInt = Color.GRAY,
            )

            with(editableTeamProvider) {
                this.incident = incident

                if (!isStale || loadedTeam != null) {
                    editableTeam.value = teamState
                }
            }

            var isEditingAllowed = true
            var isNetworkLoadFinished = true
            if (!isCreateTeam) {
                // Minimal state for editing to to begin
                isEditingAllowed = isEditingAllowed &&
                    localTeam != null
                isNetworkLoadFinished = isEditingAllowed &&
                    isPulled
            }
            TeamEditorViewState.TeamData(
                isEditingAllowed,
                teamState,
                localTeam,
                isNetworkLoadFinished,
            )
        }

    val viewState: MutableStateFlow<TeamEditorViewState> =
        MutableStateFlow(TeamEditorViewState.Loading)

    private val teamData = viewState.map(TeamEditorViewState::asTeamData)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = ReplaySubscribed3,
        )

    val isPendingSync = teamData
        .mapNotNull { it?.isPendingSync }
        .stateIn(
            scope = coroutineScope,
            initialValue = false,
            started = ReplaySubscribed3,
        )

    @OptIn(FlowPreview::class)
    private val userProfileLookup = teamData.mapNotNull { it?.team?.memberIds }
        .debounce(1.seconds)
        .distinctUntilChanged()
        .map {
            val userProfiles = usersRepository.getUserProfiles(it, true)
            userProfiles.associateBy(PersonContact::id)
        }
        .flowOn(coroutineDispatcher)

    val profilePictureLookup = userProfileLookup
        .mapLatest(::buildProfilePicLookup)
        .stateIn(
            scope = coroutineScope,
            initialValue = emptyMap(),
            started = ReplaySubscribed3,
        )

    val userRoleLookup = usersRepository.streamUserRoleLookup
        .mapLatest { lookup ->
            val updated = mutableMapOf<Int, UserRole>()
            for (entry in lookup) {
                val role = entry.value
                updated[entry.key] = role.copy(
                    name = translator.translate(role.nameKey) ?: role.nameKey,
                    description = translator.translate(role.descriptionKey) ?: role.descriptionKey,
                )
            }
            updated
        }
        .stateIn(
            scope = coroutineScope,
            initialValue = emptyMap(),
            started = ReplaySubscribed3,
        )

    init {
        if (logDebug) {
            (logger as? TagLogger)?.let {
                it.tag = debugTag
            }
        }

        coroutineScope.launch(coroutineDispatcher) {
            try {
                languageRefresher.pullLanguages()
            } catch (e: Exception) {
                logger.logException(e)
            }
        }

        coroutineScope.launch(coroutineDispatcher) {
            isRefreshingIncident.value = true
            try {
                incidentRefresher.pullIncident(incidentIdIn)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isRefreshingIncident.value = false
            }
        }

        teamStream
            .onEach {
                it?.let { localTeam ->
                    if (isInitiallySynced.getAndSet(true)) {
                        return@onEach
                    }

                    try {
                        val team = localTeam.team
                        val networkId = team.networkId
                        if (team.id > 0 &&
                            (networkId > 0 || localTeam.localChanges.isLocalModified)
                        ) {
                            isRefreshingTeam.value = true
                            if (teamChangeRepository.trySyncTeam(team.id)) {
                                // TODO Query for missing team members comparing memberIds and members
                            }
                        }
                    } finally {
                        isRefreshingTeam.value = false
                        isTeamPulled.value = true
                    }
                }
            }
            .flowOn(coroutineDispatcher)
            .launchIn(coroutineScope)

        viewStateInternal
            .onEach { viewState.value = it }
            .launchIn(coroutineScope)

        coroutineScope.launch(coroutineDispatcher) {
            userRoleRefresher.pullRoles()
        }
    }

    fun reloadData(teamId: Long) {
        editableTeamProvider.setStale()
        teamIdStream.value = teamId
        dataLoadCountStream.value++
    }
}

internal fun TeamEditorViewState.asTeamData() = this as? TeamEditorViewState.TeamData

sealed interface TeamEditorViewState {
    data object Loading : TeamEditorViewState

    data class TeamData(
        val isEditingAllowed: Boolean,
        val team: CleanupTeam,
        val localTeam: LocalTeam?,
        val isNetworkLoadFinished: Boolean,
    ) : TeamEditorViewState {
        val isPendingSync = localTeam?.localChanges?.isLocalModified ?: false
    }

    data class Error(
        val errorResId: Int = 0,
        val errorMessage: String = "",
    ) : TeamEditorViewState
}
