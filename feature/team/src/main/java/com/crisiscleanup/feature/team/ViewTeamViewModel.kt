package com.crisiscleanup.feature.team

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.ReplaySubscribed3
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.commoncase.CaseFlagsNavigationState
import com.crisiscleanup.core.commoncase.WorksiteProvider
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
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.WorkTypeChipIconProvider
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.TeamWorksiteIds
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.team.navigation.ViewTeamArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
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
    worksitesRepository: WorksitesRepository,
    worksiteChangeRepository: WorksiteChangeRepository,
    accountDataRefresher: AccountDataRefresher,
    organizationRefresher: OrganizationRefresher,
    userRoleRefresher: UserRoleRefresher,
    teamsRepository: TeamsRepository,
    private val teamChangeRepository: TeamChangeRepository,
    private val editableTeamProvider: EditableTeamProvider,
    usersRepository: UsersRepository,
    worksiteProvider: WorksiteProvider,
    workTypeChipIconProvider: WorkTypeChipIconProvider,
    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    languageRefresher: LanguageRefresher,
    private val syncPusher: SyncPusher,
    private val translator: KeyResourceTranslator,
    appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val viewTeamArgs = ViewTeamArgs(savedStateHandle)
    val incidentIdArg = viewTeamArgs.incidentId
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

    private val flagsNavigationState = CaseFlagsNavigationState(
        worksiteChangeRepository,
        worksitesRepository,
        worksiteProvider,
        viewModelScope,
        ioDispatcher,
    )
    val openWorksiteAddFlagCounter = flagsNavigationState.openWorksiteAddFlagCounter

    // TODO Consider group assign and unassign
    val isPendingCaseAction = flagsNavigationState.isLoadingFlagsWorksite
        .map { b0 -> b0 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
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
            worksitesRepository,
            userRoleRefresher,
            teamsRepository,
            teamChangeRepository,
            languageRefresher,
            usersRepository,
            translator,
            editableTeamProvider,
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
                updateHeaderTitle(it.team.name)
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

    val isPendingSync = dataLoader.isPendingSync
    val profilePictureLookup = dataLoader.profilePictureLookup
    val userRoleLookup = dataLoader.userRoleLookup

    val worksiteWorkTypeIconLookup = dataLoader.worksiteWorkTypeIconLookup
    val worksiteDistances = dataLoader.worksiteDistances

    private fun updateHeaderTitle(teamName: String = "") {
        headerTitle = if (teamName.isEmpty()) {
            translator.translate("nav.organization_teams") ?: "nav.organization_teams"
        } else {
            ""
        }
    }

    fun onOpenCaseFlags(worksite: Worksite) = flagsNavigationState.onOpenCaseFlags(worksite)

    fun takeOpenWorksiteAddFlag() = flagsNavigationState.takeOpenWorksiteAddFlag()

    fun onGroupUnassign(team: CleanupTeam, worksite: Worksite) {
        // TODO Change team unassign all assigned work
        val teamWorksite = TeamWorksiteIds(team.id, worksite.id)
        logger.logDebug("Unassign work ${worksite.workTypes} from Team $teamWorksite")
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

data class WorksiteDistance(
    val worksite: Worksite,
    val distanceMiles: Double = -1.0,
)
