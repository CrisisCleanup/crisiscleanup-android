package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.ParsedPhoneNumber
import com.crisiscleanup.core.common.utcTimeZone
import com.crisiscleanup.core.commonassets.ui.getEquipmentIcon
import com.crisiscleanup.core.commoncase.ui.CaseTableItem
import com.crisiscleanup.core.commoncase.ui.SyncStatusView
import com.crisiscleanup.core.commoncase.ui.caseItemTopRowHorizontalContentOffset
import com.crisiscleanup.core.commoncase.ui.tableItemContentPadding
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.HelpDialog
import com.crisiscleanup.core.designsystem.component.PhoneCallDialog
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemCenterSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.neutralFontColor
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.MemberEquipment
import com.crisiscleanup.core.model.data.TeamWorksiteIds
import com.crisiscleanup.core.model.data.UserRole
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.team.ViewTeamViewModel
import com.crisiscleanup.feature.team.WorksiteDistance
import kotlinx.datetime.toJavaInstant
import java.time.format.DateTimeFormatter

private val activityDateFormatter =
    DateTimeFormatter.ofPattern("MMM d yyyy").utcTimeZone

@Composable
fun ViewTeamRoute(
    onBack: () -> Unit,
    onEditTeamMembers: (Long, Long) -> Unit = { _, _ -> },
    onEditCases: (Long, Long) -> Unit = { _, _ -> },
    onEditEquipment: (Long, Long) -> Unit = { _, _ -> },
    onViewCase: (Long, Long) -> Boolean = { _, _ -> false },
    onOpenFlags: () -> Unit = {},
    onAssignCaseTeam: (Long) -> Unit = {},
    viewModel: ViewTeamViewModel = hiltViewModel(),
) {
    val openAddFlagCounter by viewModel.openWorksiteAddFlagCounter.collectAsStateWithLifecycle()
    LaunchedEffect(openAddFlagCounter) {
        if (viewModel.takeOpenWorksiteAddFlag()) {
            onOpenFlags()
        }
    }
    ViewTeamScreen(
        onBack,
        onEditTeamMembers,
        onEditCases,
        onEditEquipment,
        onViewCase,
        onAssignCaseTeam,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewTeamScreen(
    onBack: () -> Unit,
    onEditTeamMembers: (Long, Long) -> Unit = { _, _ -> },
    onEditCases: (Long, Long) -> Unit = { _, _ -> },
    onEditEquipment: (Long, Long) -> Unit = { _, _ -> },
    onViewCase: (Long, Long) -> Boolean = { _, _ -> false },
    onAssignCaseTeam: (Long) -> Unit = {},
    viewModel: ViewTeamViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isPendingCaseAction by viewModel.isPendingCaseAction.collectAsStateWithLifecycle()
    val isBusy = isLoading || isSaving

    val isEditable = !(isBusy || isPendingCaseAction)

    val title = viewModel.headerTitle

    val accountId by viewModel.accountId.collectAsStateWithLifecycle()
    val team by viewModel.editableTeam.collectAsStateWithLifecycle()
    val worksites by viewModel.worksiteDistances.collectAsStateWithLifecycle()
    val profilePictureLookup by viewModel.profilePictureLookup.collectAsStateWithLifecycle()
    val userRoleLookup by viewModel.userRoleLookup.collectAsStateWithLifecycle()
    val worksiteWorkTypeIconLookup by viewModel.worksiteWorkTypeIconLookup.collectAsStateWithLifecycle()

    val isPendingSync by viewModel.isPendingSync.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var phoneNumberList by remember { mutableStateOf(emptyList<ParsedPhoneNumber>()) }
    val setPhoneNumberList = remember(viewModel) {
        { list: List<ParsedPhoneNumber> ->
            phoneNumberList = list
        }
    }

    val incidentId = viewModel.incidentIdArg

    val onCaseSelect = remember(viewModel) {
        { worksite: Worksite ->
            onViewCase(incidentId, worksite.id)
        }
    }
    val openEditMembers = remember(team.id) { { onEditTeamMembers(incidentId, team.id) } }
    val openEditCases = remember(team.id) { { onEditCases(incidentId, team.id) } }
    val openEditEquipment = remember(team.id) { { onEditEquipment(incidentId, team.id) } }
    val assignCaseTeam = remember(viewModel) {
        { worksite: Worksite ->
            onAssignCaseTeam(worksite.id)
        }
    }

    Box {
        Column {
            // TODO Animate team name to title on scroll up when team name is defined
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = { TopBarBackAction(onBack) },
                actions = { /* TODO */ },
            )

            ViewTeamContent(
                accountId,
                team,
                profilePictureLookup,
                userRoleLookup,
                worksites,
                worksiteWorkTypeIconLookup,
                isEditable = isEditable,
                isSyncing = isSyncing,
                isPendingSync = isPendingSync,
                scheduleSync = viewModel::scheduleSync,
                onEditTeamMembers = openEditMembers,
                onEditCases = openEditCases,
                onEditEquipment = openEditEquipment,
                onViewCase = onCaseSelect,
                onOpenFlags = viewModel::onOpenCaseFlags,
                onAssignCaseTeam = assignCaseTeam,
                showPhoneNumbers = setPhoneNumberList,
                onGroupUnassign = viewModel::onGroupUnassign,
            )
        }

        BusyIndicatorFloatingTopCenter(isBusy)

        val clearPhoneNumbers = remember(viewModel) { { setPhoneNumberList(emptyList()) } }
        PhoneCallDialog(
            phoneNumberList,
            clearPhoneNumbers,
        )
    }
}

// TODO Common styles
private val headerTextStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalFontStyles.current.header3

@Composable
private fun EditSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    linkActionModifier: Modifier = Modifier,
    enabled: Boolean = false,
    showEditAction: Boolean = false,
    color: Color = primaryBlueColor,
    action: () -> Unit = {},
) {
    Row(
        Modifier.listRowItemStartPadding(),
        horizontalArrangement = listItemSpacedByHalf,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier,
            style = headerTextStyle,
        )

        Spacer(Modifier.weight(1f))

        if (showEditAction) {
            Box(
                modifier = Modifier
                    .clickable(
                        enabled = enabled,
                        onClick = action,
                    )
                    .then(linkActionModifier),
            ) {
                Text(
                    LocalAppTranslator.current("actions.edit"),
                    // TODO Common dimensions
                    Modifier.padding(16.dp),
                    style = LocalFontStyles.current.header4,
                    color = if (enabled) color else color.disabledAlpha(),
                )
            }
        }
    }
}

@Composable
private fun ViewTeamContent(
    accountId: Long,
    team: CleanupTeam,
    profilePictureLookup: Map<Long, String>,
    userRoleLookup: Map<Int, UserRole>,
    worksites: List<WorksiteDistance>,
    worksiteWorkTypeIconLookup: Map<Long, List<ImageBitmap>>,
    isEditable: Boolean,
    isSyncing: Boolean,
    isPendingSync: Boolean,
    scheduleSync: () -> Unit,
    onEditTeamMembers: () -> Unit = {},
    onEditCases: () -> Unit = {},
    onEditEquipment: () -> Unit = {},
    onViewCase: (Worksite) -> Boolean = { _ -> false },
    onOpenFlags: (Worksite) -> Unit = {},
    onAssignCaseTeam: (Worksite) -> Unit = {},
    showPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit = {},
    onGroupUnassign: (CleanupTeam, Worksite) -> Unit = { _, _ -> },
) {
    val t = LocalAppTranslator.current

    var errorMessage by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = listItemSpacedBy,
    ) {
        item {
            TeamHeader(
                team,
                isSyncing = isSyncing,
                isPendingSync = isPendingSync,
                scheduleSync = scheduleSync,
            )
        }

        item(
            key = "team-member-header",
            contentType = "header-item",
        ) {
            val sectionTitle = t("~~Team Members ({team_size})")
                .replace("{team_size}", "${team.members.size}")
            EditSectionHeader(
                sectionTitle,
                enabled = isEditable,
                showEditAction = true,
                action = onEditTeamMembers,
            )
        }

        items(
            team.members,
            key = { "member-${it.id}" },
            contentType = { "member-item" },
        ) {
            TeamMemberContactCardView(
                it,
                profilePictureLookup,
                userRoleLookup,
                it.id != accountId,
            ) { message ->
                errorMessage = message
            }
        }

        item(
            key = "work-header",
            contentType = "header-item",
        ) {
            val sectionTitle = t("~~Assigned Cases ({case_count})")
                .replace("{case_count}", "${worksites.size}")
            EditSectionHeader(
                sectionTitle,
                enabled = isEditable,
                showEditAction = true,
                action = onEditCases,
            )
        }

        if (team.missingWorkTypeCount > 0) {
            // TODO Notify if there are missing work types and loading is done
        }

        items(
            worksites,
            key = { "worksite-${it.worksite.id}" },
            contentType = { "worksite-item" },
        ) {
            TeamWorksiteView(
                team,
                it,
                worksiteWorkTypeIconLookup[it.worksite.id] ?: emptyList(),
                onViewCase = onViewCase,
                onOpenFlags = onOpenFlags,
                isEditable = isEditable,
                // TODO Set and consider assigning/unassigning team Worksites
                // transientTeamWorksiteAssignments = ,
                showPhoneNumbers = showPhoneNumbers,
                onAssignTeam = onAssignCaseTeam,
                onGroupUnassign = onGroupUnassign,
            )
        }

        item(
            key = "equipment-header",
            contentType = "header-item",
        ) {
            val sectionTitle = t("~~Assets ({asset_count})")
                .replace("{asset_count}", "${team.memberEquipment.size}")
            EditSectionHeader(
                sectionTitle,
                enabled = isEditable,
                showEditAction = true,
                action = onEditEquipment,
            )
        }

        items(
            team.memberEquipment,
            key = { "equipment-${it.userId}-${it.equipmentData.id}" },
            contentType = { "equipment-item" },
        ) {
            TeamMemberEquipmentView(it)
        }

        item(
            key = "statistics-header",
            contentType = "header-item",
        ) {
            val sectionTitle = t("~~Statistics")
            EditSectionHeader(
                sectionTitle,
                enabled = isEditable,
                action = onEditEquipment,
            )
        }

        item(
            key = "statistics-view",
        ) {
            TeamStatisticsView(team)
        }

        item {
            Spacer(Modifier.listItemBottomPadding())
        }
    }

    if (errorMessage.isNotBlank()) {
        HelpDialog(
            title = t("info.error"),
            text = errorMessage,
            onClose = { errorMessage = "" },
        )
    }
}

@Composable
private fun TeamHeader(
    team: CleanupTeam,
    isSyncing: Boolean = false,
    isPendingSync: Boolean = false,
    scheduleSync: () -> Unit,
) {
    val t = LocalAppTranslator.current
    val modifier = if (isSyncing || !isPendingSync) {
        fillWidthPadded
    } else {
        Modifier.fillMaxWidth()
            // TODO Common dimensions
            .padding(vertical = 16.dp)
            .padding(start = 16.dp)
    }
    Column(
        // TODO Common colors
        Modifier.background(Color.White)
            .then(modifier),
        verticalArrangement = listItemSpacedByHalf,
    ) {
        Row(
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamColorView(team.colorInt)

            Text(
                team.name.ifBlank { t("~~(no name)") },
                style = LocalFontStyles.current.header2,
            )

            Spacer(Modifier.weight(1f))

            SyncStatusView(
                isSyncing = isSyncing,
                isPendingSync = isPendingSync,
                scheduleSync = scheduleSync,
            )
        }

        val openCaseCount = team.caseOpenCount
        Row(
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (team.caseCount == 0) {
                Text(t("~~0 Cases"))
            } else {
                val caseCountTranslateKey =
                    if (openCaseCount == 1) "~~{case_count} Open Case" else "~~{case_count} Open Cases"
                Text(
                    t(caseCountTranslateKey)
                        .replace("{case_count}", "$openCaseCount"),
                )
            }

            TeamCaseCompleteView(team)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamWorksiteView(
    team: CleanupTeam,
    worksiteDistance: WorksiteDistance,
    workTypeIcons: List<ImageBitmap>,
    onViewCase: (Worksite) -> Boolean = { _ -> false },
    onOpenFlags: (Worksite) -> Unit = {},
    isEditable: Boolean = false,
    transientTeamWorksiteAssignments: Set<TeamWorksiteIds> = emptySet(),
    showPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit = {},
    onAssignTeam: (Worksite) -> Unit = {},
    onGroupUnassign: (CleanupTeam, Worksite) -> Unit = { _, _ -> },
) {
    val worksite = worksiteDistance.worksite
    val distance = worksiteDistance.distanceMiles
    val teamWorksite = TeamWorksiteIds(team.id, worksite.id)

    val workTypesRow = @Composable {
        FlowRow(
            Modifier.offset(x = caseItemTopRowHorizontalContentOffset),
            verticalArrangement = listItemCenterSpacedByHalf,
            horizontalArrangement = listItemSpacedByHalf,
        ) {
            workTypeIcons.forEach {
                Image(
                    it,
                    // TODO Content descriptions
                    contentDescription = null,
                )
            }
        }
    }
    CardSurface {
        CaseTableItem(
            worksite,
            distance,
            listItemModifier,
            onViewCase = { onViewCase(worksite) },
            onOpenFlags = { onOpenFlags(worksite) },
            onAssignToTeam = { onAssignTeam(worksite) },
            isEditable = isEditable,
            showPhoneNumbers = showPhoneNumbers,
            upperContent = workTypesRow,
            // TODO Adjust to view width
            isUpperContentAtTop = workTypeIcons.size < 6,
        ) {
            Spacer(Modifier.weight(1f))

            CrisisCleanupOutlinedButton(
                onClick = { onGroupUnassign(team, worksite) },
                enabled = isEditable && !transientTeamWorksiteAssignments.contains(teamWorksite),
                contentPadding = tableItemContentPadding,
            ) {
                Text(
                    LocalAppTranslator.current("~~Unassign"),
                )
            }
        }
    }
}

@Composable
private fun TeamMemberEquipmentView(
    memberEquipment: MemberEquipment,
) {
    val t = LocalAppTranslator.current
    val equipmentName = t(memberEquipment.equipmentData.equipment.literal)
    CardSurface {
        Row(
            listItemModifier,
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painterResource(getEquipmentIcon(memberEquipment.equipmentData.equipment)),
                equipmentName,
                tint = neutralIconColor,
            )

            Column {
                Text(
                    equipmentName,
                    style = LocalFontStyles.current.header4,
                )
                Text(memberEquipment.userName)
            }
        }
    }
}

@Composable
private fun TeamStatisticsView(
    team: CleanupTeam,
) {
    val t = LocalAppTranslator.current

    CardSurface {
        Row(
            fillWidthPadded,
            horizontalArrangement = listItemSpacedBy,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${team.workCompletePercentage}%",
                    style = LocalFontStyles.current.titleStatistics,
                )
                Text(
                    t("~~Completion rate"),
                    style = LocalFontStyles.current.header4,
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = listItemSpacedByHalf,
            ) {
                Text(
                    t("~~{case_count} case(s) closed")
                        .replace("{case_count}", "${team.caseCompleteCount}"),
                )
                Text(
                    t("~~{case_count} open case(s)")
                        .replace("{case_count}", "${team.caseOpenCount}"),
                )
                Text(
                    t("~~{case_count} overdue case(s)")
                        .replace("{case_count}", "${team.caseOpenCount}"),
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    val dateStyle = LocalFontStyles.current.header3
    CardSurface {
        Row(
            fillWidthPadded,
            horizontalArrangement = listItemSpacedBy,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            team.firstActivityDate?.let { firstActivity ->
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = listItemSpacedByHalf,
                ) {
                    Text(
                        activityDateFormatter.format(firstActivity.toJavaInstant()),
                        style = dateStyle,
                    )
                    Text(
                        t("~~First Activity"),
                        color = neutralFontColor,
                    )
                }
            }
            team.lastActivityDate?.let { lastActivity ->
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = listItemSpacedByHalf,
                ) {
                    Text(
                        activityDateFormatter.format(lastActivity.toJavaInstant()),
                        style = dateStyle,
                    )
                    Text(
                        t("~~Last Activity"),
                        color = neutralFontColor,
                    )
                }
            }
        }
    }
}
