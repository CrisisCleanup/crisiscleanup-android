package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.ParsedPhoneNumber
import com.crisiscleanup.core.common.openDialer
import com.crisiscleanup.core.common.openEmail
import com.crisiscleanup.core.common.openSms
import com.crisiscleanup.core.commoncase.ui.CaseTableItem
import com.crisiscleanup.core.commoncase.ui.SyncStatusView
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AvatarIcon
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.HelpDialog
import com.crisiscleanup.core.designsystem.component.PhoneCallDialog
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.UserRole
import com.crisiscleanup.feature.team.ViewTeamViewModel
import com.crisiscleanup.feature.team.WorksiteDistance

@Composable
fun ViewTeamRoute(
    onBack: () -> Unit,
    onViewCase: () -> Unit = {},
    onOpenFlags: () -> Unit = {},
) {
    ViewTeamScreen(
        onBack,
        onViewCase,
        onOpenFlags,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewTeamScreen(
    onBack: () -> Unit,
    onViewCase: () -> Unit = {},
    onOpenFlags: () -> Unit = {},
    viewModel: ViewTeamViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isBusy = isLoading || isSaving

    val isEditable = !isBusy

    val title = viewModel.headerTitle

    val accountId by viewModel.accountId.collectAsStateWithLifecycle()
    val team by viewModel.editableTeam.collectAsStateWithLifecycle()
    val worksites by viewModel.worksites.collectAsStateWithLifecycle()
    val profilePictureLookup by viewModel.profilePictureLookup.collectAsStateWithLifecycle()
    val userRoleLookup by viewModel.userRoleLookup.collectAsStateWithLifecycle()

    val isPendingSync by viewModel.isPendingSync.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var phoneNumberList by remember { mutableStateOf(emptyList<ParsedPhoneNumber>()) }
    val setPhoneNumberList = remember(viewModel) {
        { list: List<ParsedPhoneNumber> ->
            phoneNumberList = list
        }
    }

    Box {
        Column {
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
                isEditable = isEditable,
                isSyncing = isSyncing,
                isPendingSync = isPendingSync,
                scheduleSync = viewModel::scheduleSync,
                onViewCase = onViewCase,
                onOpenFlags = onOpenFlags,
                showPhoneNumbers = setPhoneNumberList,
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
    isEditable: Boolean,
    isSyncing: Boolean,
    isPendingSync: Boolean,
    scheduleSync: () -> Unit,
    onEditTeamMembers: () -> Unit = {},
    onEditCases: () -> Unit = {},
    onViewCase: () -> Unit = {},
    onOpenFlags: () -> Unit = {},
    showPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit = {},
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
            TeamMemberCardView(
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
                it,
                onViewCase = onViewCase,
                onOpenFlags = onOpenFlags,
                isEditable = isEditable,
                showPhoneNumbers = showPhoneNumbers,
            )
        }

        // TODO Assets

        // TODO Statistics

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

        val openCaseCount = team.openCaseCount
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

@Composable
private fun TeamMemberCardView(
    person: PersonContact,
    profilePictureLookup: Map<Long, String>,
    userRoleLookup: Map<Int, UserRole>,
    showActions: Boolean,
    onActionError: (String) -> Unit = {},
) {
    val t = LocalAppTranslator.current

    var contentSize by remember { mutableStateOf(Size.Zero) }
    CardSurface {
        Row(
            // TODO Common dimensions
            Modifier.padding(12.dp)
                .onGloballyPositioned {
                    contentSize = it.size.toSize()
                },
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = if (person.activeRoles.isEmpty()) {
                Alignment.CenterVertically
            } else {
                Alignment.Top
            },
        ) {
            Box(
                modifier = Modifier
                    .size(LocalDimensions.current.avatarCircleRadius)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AvatarIcon(
                    profilePictureLookup[person.id] ?: person.profilePictureUri,
                    person.fullName,
                )
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = listItemSpacedByHalf,
            ) {
                Text(
                    person.fullName,
                    style = LocalFontStyles.current.header4,
                )
                for (userRoleCode in person.activeRoles) {
                    val role = userRoleLookup[userRoleCode]?.name ?: ""
                    if (role.isNotBlank()) {
                        Text(
                            role,
                            style = LocalFontStyles.current.helpTextStyle,
                        )
                    }
                }
            }

            if (showActions) {
                val context = LocalContext.current
                val phoneNumber = person.mobile.trim()
                val hasPhone = phoneNumber.isNotBlank()
                if (hasPhone) {
                    CrisisCleanupIconButton(
                        imageVector = CrisisCleanupIcons.Phone,
                        onClick = {
                            if (!context.openDialer(phoneNumber)) {
                                onActionError(t("~~This device does not support phone calls."))
                            }
                        },
                        tint = neutralIconColor,
                        contentDescription = t("actions.call"),
                    )
                    CrisisCleanupIconButton(
                        imageVector = CrisisCleanupIcons.Sms,
                        onClick = {
                            if (!context.openSms(phoneNumber)) {
                                onActionError(t("~~This device does not support text messaging."))
                            }
                        },
                        tint = neutralIconColor,
                        contentDescription = t("actions.chat"),
                    )
                }
                val emailAddress = person.email.trim()
                if (emailAddress.isNotBlank()) {
                    val openEmailOrError = remember(emailAddress) {
                        {
                            if (!context.openEmail(emailAddress)) {
                                onActionError(t("~~This device does not support sending emails."))
                            }
                        }
                    }
                    if (hasPhone) {
                        TeamMemberOverflowMenu(contentSize, openEmailOrError)
                    } else {
                        CrisisCleanupIconButton(
                            imageVector = CrisisCleanupIcons.Email,
                            onClick = openEmailOrError,
                            tint = neutralIconColor,
                            contentDescription = t("actions.chat"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamMemberOverflowMenu(
    contentSize: Size,
    onOpenEmail: () -> Unit,
) {
    val t = LocalAppTranslator.current
    var showDropdown by remember { mutableStateOf(false) }
    Box {
        CrisisCleanupIconButton(
            imageVector = CrisisCleanupIcons.MoreVert,
            onClick = { showDropdown = true },
            tint = neutralIconColor,
            contentDescription = t("actions.show_more"),
        )

        if (showDropdown) {
            DropdownMenu(
                modifier = Modifier
                    .sizeIn(maxWidth = 128.dp)
                    .width(
                        with(LocalDensity.current) {
                            contentSize.width.toDp()
                        },
                    ),
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                DropdownMenuItem(
                    modifier = Modifier.optionItemHeight(),
                    text = { Text(t("actions.email")) },
                    onClick = {
                        onOpenEmail()
                        showDropdown = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TeamWorksiteView(
    worksiteDistance: WorksiteDistance,
    onViewCase: () -> Unit = {},
    onOpenFlags: () -> Unit = {},
    isEditable: Boolean = false,
    showPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit = {},
) {
    val worksite = worksiteDistance.worksite
    val distance = worksiteDistance.distanceMiles

    CardSurface {
        CaseTableItem(
            worksite,
            distance,
            listItemModifier,
            onViewCase = onViewCase,
            onOpenFlags = onOpenFlags,
            isEditable = isEditable,
            showPhoneNumbers = showPhoneNumbers,
        ) {
            // TODO Unassign Case from team
        }
    }
}
