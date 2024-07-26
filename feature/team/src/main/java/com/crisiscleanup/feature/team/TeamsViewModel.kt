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
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentTeams
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@HiltViewModel
class TeamViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
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
        worksitesRepository,
        incidentSelector,
        translator,
        accountDataRepository,
        appPreferencesRepository,
        viewModelScope,
    )
    val incidentsData = appTopBarDataProvider.incidentsData
    val loadSelectIncidents = appTopBarDataProvider.loadSelectIncidents

    val viewState = incidentSelector.incidentId
        .flatMapLatest { incidentId ->
            if (incidentId == EmptyIncident.id) {
                return@flatMapLatest flowOf(TeamsViewState.Loading)
            }

            teamsRepository.streamIncidentTeams(incidentId).mapLatest { teams ->
                TeamsViewState.Success(
                    incidentId,
                    teams,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = TeamsViewState.Loading,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    val isLoading = viewState.map { it == TeamsViewState.Loading }

    // TODO Get my teams, not my teams in this incident

    private val profilePictureLookup = ConcurrentHashMap<Long, String>()
    private val pendingProfileUserIdsFlow = MutableStateFlow(emptySet<Long>())
    private val pendingProfileUserIds = ConcurrentHashMap<Long, Boolean>()

    init {
        viewModelScope.launch(ioDispatcher) {
            pendingProfileUserIdsFlow
                .debounce(1.seconds)
                .onEach { ids ->
                    val missingProfilePictures = ids.filter { !profilePictureLookup.contains(it) }
                    if (missingProfilePictures.isNotEmpty()) {
                        val userProfiles =
                            usersRepository.getUserProfiles(missingProfilePictures, true)
                        for (profile in userProfiles) {
                            profilePictureLookup[profile.id] = profile.profilePictureUri
                        }
                        for (userId in ids) {
                            pendingProfileUserIds.remove(userId)
                        }
                    }
                }
        }

        viewModelScope.launch(ioDispatcher) {
            incidentSelector.incidentId.filter { it != EmptyIncident.id }
                .distinctUntilChanged()
                .onEach {
                    if (it != EmptyIncident.id) {
                        teamsRepository.syncTeams(it)
                    }
                }
        }
    }

    suspend fun refreshIncidents() {
        syncPuller.pullIncidents()
    }

    fun queryUserProfilePic(userId: Long) {
        if (profilePictureLookup.contains(userId)) {
            return
        }

        if (!pendingProfileUserIds.contains(userId)) {
            pendingProfileUserIds[userId] = true
            pendingProfileUserIdsFlow.value = pendingProfileUserIds.keys.toSet()
        }
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
    ) : TeamsViewState
}
