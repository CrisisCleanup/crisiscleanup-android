package com.crisiscleanup.feature.team

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.ReplaySubscribed3
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.IncidentRefresher
import com.crisiscleanup.core.data.LanguageRefresher
import com.crisiscleanup.core.data.OrganizationRefresher
import com.crisiscleanup.core.data.UserRoleRefresher
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.TeamChangeRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.feature.team.navigation.ViewTeamArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    accountDataRefresher: AccountDataRefresher,
    organizationRefresher: OrganizationRefresher,
    userRoleRefresher: UserRoleRefresher,
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
) : ViewModel() {
    private val viewTeamArgs = ViewTeamArgs(savedStateHandle)
    private val incidentIdArg = viewTeamArgs.incidentId
    private val teamIdArg = viewTeamArgs.teamId

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
            started = ReplaySubscribed3,
        )

    private val isSavingTeam = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = isSavingTeam
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = ReplaySubscribed3,
        )

    val editableTeam = editableTeamProvider.editableTeam

    val accountId = accountDataRepository.accountData.map { it.id }
        .stateIn(
            scope = viewModelScope,
            initialValue = 0L,
            started = ReplaySubscribed3,
        )

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
            userRoleRefresher,
            teamsRepository,
            teamChangeRepository,
            languageRefresher,
            usersRepository,
            translator,
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

        // TODO Are below necessary or leftover from copy-paste?

        viewModelScope.launch(ioDispatcher) {
            accountDataRefresher.updateMyOrganization(false)
        }

        viewModelScope.launch(ioDispatcher) {
            organizationRefresher.pullOrganization(incidentIdArg)
        }
    }

    val isLoading = dataLoader.isLoading

    val viewState = dataLoader.viewState
    val isPendingSync = dataLoader.isPendingSync
    val profilePictureLookup = dataLoader.profilePictureLookup
    val userRoleLookup = dataLoader.userRoleLookup

    private fun updateHeaderTitle(teamName: String = "") {
        headerTitle = if (teamName.isEmpty()) {
            translator.translate("nav.organization_teams") ?: "nav.organization_teams"
        } else {
            ""
        }
    }

    fun scheduleSync() {
        // TODO
    }

    private fun saveTeamChange(
        startingTeam: CleanupTeam,
        changedTeam: CleanupTeam,
        onSaveAction: () -> Unit = {},
    ) {
        // TODO As necessary
    }
}
