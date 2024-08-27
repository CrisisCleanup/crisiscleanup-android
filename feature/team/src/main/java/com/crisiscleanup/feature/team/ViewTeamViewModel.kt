package com.crisiscleanup.feature.team

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.IncidentRefresher
import com.crisiscleanup.core.data.LanguageRefresher
import com.crisiscleanup.core.data.OrganizationRefresher
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.TeamChangeRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.feature.team.navigation.ViewTeamArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ViewTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    private val incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    accountDataRefresher: AccountDataRefresher,
    organizationRefresher: OrganizationRefresher,
    teamsRepository: TeamsRepository,
    private val teamChangeRepository: TeamChangeRepository,
    private val editableTeamProvider: EditableTeamProvider,
    usersRepository: UsersRepository,
    private val syncPusher: SyncPusher,
    languageRefresher: LanguageRefresher,
    private val translator: KeyResourceTranslator,
    appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), KeyResourceTranslator {
    private val viewTeamArgs = ViewTeamArgs(savedStateHandle)
    private val incidentIdArg = viewTeamArgs.incidentId
    val teamIdArg = viewTeamArgs.teamId

    var headerTitle by mutableStateOf("")
        private set

    private val dataLoader: TeamEditorDataLoader

    val isSyncing = teamChangeRepository.syncingTeamIds
        .mapLatest { syncingTeamIds ->
            syncingTeamIds.contains(teamIdArg)
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(3.seconds.inWholeMilliseconds),
        )

    private val isSavingTeam = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = isSavingTeam
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val editableTeam = editableTeamProvider.editableTeam

    init {
        updateHeaderTitle()

        editableTeamProvider.reset(incidentIdArg)

        dataLoader = TeamEditorDataLoader(
            false,
            incidentIdArg,
            teamIdArg,
            accountDataRepository,
            incidentsRepository,
            incidentRefresher,
            teamsRepository,
            teamChangeRepository,
            languageRefresher,
            { key -> translate(key) },
            editableTeamProvider,
            viewModelScope,
            ioDispatcher,
            appEnv,
            logger,
        )

        dataLoader.teamStream
            .onEach {
                it?.let { cachedTeam ->
                    updateHeaderTitle(cachedTeam.team.name)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            accountDataRefresher.updateMyOrganization(false)
        }

        viewModelScope.launch(ioDispatcher) {
            organizationRefresher.pullOrganization(incidentIdArg)
        }
    }

    val isLoading = dataLoader.isLoading

    private val viewState = dataLoader.viewState

    private val referenceTeam: CleanupTeam
        get() = viewState.value.asTeamData()?.team ?: EmptyCleanupTeam

    val teamData = viewState.map(TeamEditorViewState::asTeamData)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    @OptIn(FlowPreview::class)
    private val userProfileLookup = teamData.mapNotNull { it?.team?.memberIds }
        .debounce(1.seconds)
        .distinctUntilChanged()
        .map {
            withContext(ioDispatcher) {
                val userProfiles = usersRepository.getUserProfiles(it, true)
                userProfiles.associateBy(PersonContact::id)
            }
        }

    val profilePictureLookup = userProfileLookup
        .mapLatest(::buildProfilePicLookup)
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyMap(),
            started = SharingStarted.WhileSubscribed(3.seconds.inWholeMilliseconds),
        )

    private fun updateHeaderTitle(teamName: String = "") {
        headerTitle = if (teamName.isEmpty()) {
            translate("nav.organization_teams")
        } else {
            ""
        }
    }

    private fun saveTeamChange(
        startingTeam: CleanupTeam,
        changedTeam: CleanupTeam,
        onSaveAction: () -> Unit = {},
    ) {
        // TODO As necessary
    }

    // KeyResourceTranslator

    override val translationCount = translator.translationCount

    override fun translate(phraseKey: String) = translate(phraseKey, 0)

    override fun translate(phraseKey: String, fallbackResId: Int) =
        translator.translate(phraseKey, fallbackResId)
}
