package com.crisiscleanup.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appcomponent.AppTopBarDataProvider
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.svgAvatarUrl
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import com.crisiscleanup.core.data.repository.IncidentTeams
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.PersonContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@HiltViewModel
class TeamsViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    incidentCacheRepository: IncidentCacheRepository,
    val incidentSelector: IncidentSelector,
    accountDataRepository: AccountDataRepository,
    appPreferencesRepository: LocalAppPreferencesRepository,
    private val teamsRepository: TeamsRepository,
    usersRepository: UsersRepository,
    private val syncPuller: SyncPuller,
    translator: KeyResourceTranslator,
    @Dispatcher(CrisisCleanupDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
) : ViewModel() {
    val appTopBarDataProvider = AppTopBarDataProvider(
        "nav.organization_teams",
        incidentsRepository,
        incidentCacheRepository,
        incidentSelector,
        translator,
        accountDataRepository,
        viewModelScope,
    )
    val incidentsData = appTopBarDataProvider.incidentsData

    private val additionalUserProfileLookup = MutableStateFlow<Map<Long, PersonContact>>(emptyMap())
    val viewState = incidentSelector.incidentId
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
            started = SharingStarted.WhileSubscribed(3_000),
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
            fun getProfilePictureUri(contact: PersonContact): String {
                var pictureUri = contact.profilePictureUri
                if (pictureUri.isBlank() && contact.fullName.isNotBlank()) {
                    pictureUri = contact.fullName.svgAvatarUrl
                }
                return pictureUri
            }

            val lookup = mutableMapOf<Long, String>()
            for (entry in (state as TeamsViewState.Success).profileLookup) {
                lookup[entry.key] = getProfilePictureUri(entry.value)
            }
            for (entry in additional) {
                lookup[entry.key] = getProfilePictureUri(entry.value)
            }
            lookup
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(3_000),
        )

    val isLoading = viewState.map { it == TeamsViewState.Loading }

    init {
        viewState
            .debounce(1.seconds)
            .onEach { state ->
                withContext(ioDispatcher) {
                    (state as? TeamsViewState.Success)?.missingProfileUserIds?.let { ids ->
                        if (ids.isNotEmpty()) {
                            val userProfiles = usersRepository.getUserProfiles(ids, true)
                            additionalUserProfileLookup.value =
                                userProfiles.associateBy(PersonContact::id)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

        incidentSelector.incidentId.filter { it != EmptyIncident.id }
            .debounce(0.2.seconds)
            .distinctUntilChanged()
            .onEach {
                if (it != EmptyIncident.id) {
                    teamsRepository.syncTeams(it)
                }
            }
            .launchIn(viewModelScope)
    }

    suspend fun refreshIncidentsAsync() {
        syncPuller.syncPullIncidents()
    }

    suspend fun refreshTeams() {
        teamsRepository.syncTeams(incidentSelector.incidentId.value)
    }
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
