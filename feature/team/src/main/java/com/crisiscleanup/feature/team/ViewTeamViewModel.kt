package com.crisiscleanup.feature.team

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.QrCodeGenerator
import com.crisiscleanup.core.common.ReplaySubscribed3
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.common.utcTimeZone
import com.crisiscleanup.core.commoncase.CaseFlagsNavigationState
import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.data.IncidentRefresher
import com.crisiscleanup.core.data.LanguageRefresher
import com.crisiscleanup.core.data.OrganizationRefresher
import com.crisiscleanup.core.data.UserRoleRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.OrgVolunteerRepository
import com.crisiscleanup.core.data.repository.TeamChangeRepository
import com.crisiscleanup.core.data.repository.TeamsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.WorkTypeChipIconProvider
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.JoinOrgTeamInvite
import com.crisiscleanup.core.model.data.TeamWorksiteIds
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.team.navigation.ViewTeamArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val expirationDateFormatter =
    DateTimeFormatter.ofPattern("h:mm a 'of' MMM d yyyy").utcTimeZone
private val expirationTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a").utcTimeZone

@HiltViewModel
class ViewTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    settingsProvider: AppSettingsProvider,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    worksitesRepository: WorksitesRepository,
    worksiteChangeRepository: WorksiteChangeRepository,
    organizationRefresher: OrganizationRefresher,
    userRoleRefresher: UserRoleRefresher,
    teamsRepository: TeamsRepository,
    orgVolunteerRepository: OrgVolunteerRepository,
    private val teamChangeRepository: TeamChangeRepository,
    private val editableTeamProvider: EditableTeamProvider,
    usersRepository: UsersRepository,
    worksiteProvider: WorksiteProvider,
    workTypeChipIconProvider: WorkTypeChipIconProvider,
    permissionManager: PermissionManager,
    locationProvider: LocationProvider,
    languageRefresher: LanguageRefresher,
    qrCodeGenerator: QrCodeGenerator,
    private val syncPusher: SyncPusher,
    private val translator: KeyResourceTranslator,
    appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), EditableTeamDataGuarder {
    private val viewTeamArgs = ViewTeamArgs(savedStateHandle)
    val incidentIdArg = viewTeamArgs.incidentId
    private val teamIdArg = viewTeamArgs.teamId

    var headerTitle by mutableStateOf("")
        private set

    // TODO Configure receiver in manifest and elsewhere
    private val inviteUrl = "${settingsProvider.baseUrl}/mobile_app_user_team_invite"

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

    // TODO Size QR codes relative to min screen dimension
    //      See Display#getSize or WindowMetrics#getBounds
    private val qrCodeSize = 512 + 256
    private val refreshQrCodeTicker = MutableStateFlow(0L)
    private val joinTeamInvite = MutableStateFlow<JoinOrgTeamInvite?>(null)
    private val systemTimeZone = TimeZone.currentSystemDefault()
    val scanQrCodeHelpText = joinTeamInvite
        .filterNotNull()
        .map {
            val isSameDay = it.expiresAt.toLocalDateTime(systemTimeZone).dayOfMonth ==
                Clock.System.now().toLocalDateTime(systemTimeZone).dayOfMonth
            val dateTimeFormatter = if (isSameDay) {
                expirationTimeFormatter
            } else {
                expirationDateFormatter
            }
            val expirationDate = dateTimeFormatter.format(it.expiresAt.toJavaInstant())
            translator("~~Scan the QR code to join this team. The code stops working on (or before) {expiration}.")
                .replace("{expiration}", expirationDate)
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )
    val joinTeamQrCode = combine(
        accountId,
        joinTeamInvite,
        ::Pair,
    )
        .filter { (_, invite) ->
            invite != null
        }
        .map { (accountId, invite) ->
            if (invite?.isExpired == false) {
                val inviteUrl = makeInviteUrl(accountId, invite)
                return@map qrCodeGenerator.generate(inviteUrl, qrCodeSize)?.asImageBitmap()
            }

            null
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
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
                updateHeaderTitle(it.team.name)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            organizationRefresher.pullOrganization(incidentIdArg)
        }

        combine(
            accountDataRepository.accountData,
            refreshQrCodeTicker
                .throttleLatest(3.seconds.inWholeMilliseconds)
                .distinctUntilChanged(),
            ::Pair,
        )
            .filter { (_, ticker) ->
                ticker > 0
            }
            .map { (accountData, _) ->
                joinTeamInvite.value?.let { invite ->
                    if (invite.targetId == teamIdArg &&
                        invite.expiresAt > Clock.System.now().plus(5.minutes)
                    ) {
                        return@map invite
                    }
                }

                orgVolunteerRepository.getTeamInvite(teamIdArg, accountData.id)
            }
            .onEach {
                joinTeamInvite.value = it
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)
    }

    val isLoading = dataLoader.isLoading

    val isPendingSync = dataLoader.isPendingSync

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

    fun onJoinLeaveTeam() {
        // TODO Push team change (join/leave) and update loading
    }

    private fun makeInviteUrl(userId: Long, invite: JoinOrgTeamInvite): String {
        val q = listOf(
            "team-id=${invite.targetId}",
            "user-id=$userId",
            "invite-token=${invite.token}",
        ).joinToString("&")
        return "$inviteUrl?$q"
    }

    fun onRefreshQrCode() {
        refreshQrCodeTicker.value = Clock.System.now().epochSeconds
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

    // EditableTeamDataGuarder

    override val isEditableTeamOpen = true
}

data class WorksiteDistance(
    val worksite: Worksite,
    val distanceMiles: Double = -1.0,
)
