package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.crisiscleanup.core.common.openDialer
import com.crisiscleanup.core.common.openEmail
import com.crisiscleanup.core.common.openSms
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AvatarIcon
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.HelpDialog
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.feature.team.ViewTeamViewModel

@Composable
fun ViewTeamRoute(
    onBack: () -> Unit,
) {
    ViewTeamScreen(
        onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewTeamScreen(
    onBack: () -> Unit,
    viewModel: ViewTeamViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isBusy = isLoading || isSaving

    val isEditable = !isBusy

    val title = viewModel.headerTitle

    val accountId by viewModel.accountId.collectAsStateWithLifecycle()
    val team by viewModel.editableTeam.collectAsStateWithLifecycle()
    val profilePictureLookup by viewModel.profilePictureLookup.collectAsStateWithLifecycle()

    val isPendingSync by viewModel.isPendingSync.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

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
                // TODO isSaving instead?
                isBusy = isBusy,
                isEditable = isEditable,
                isPendingSync = isPendingSync,
                isSyncing = isSyncing,
            )
        }
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
    isBusy: Boolean,
    isEditable: Boolean,
    isPendingSync: Boolean,
    isSyncing: Boolean,
    onEditTeamMembers: () -> Unit = {},
) {
    val t = LocalAppTranslator.current

    var errorMessage by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = listItemSpacedBy,
    ) {
        item {
            TeamHeader(
                team,
                isPendingSync = isPendingSync,
                isSyncing = isSyncing,
            )
        }

        item(
            // TODO Key and type
        ) {
            val teamMemberSectionTitle = t("~~Team Members ({team_size})")
                .replace("{team_size}", "${team.members.count()}")
            EditSectionHeader(
                teamMemberSectionTitle,
                enabled = isEditable,
                showEditAction = true,
                action = onEditTeamMembers,
            )
        }

        items(
            team.members,
            key = { it.id },
            contentType = { "member-item" },
        ) {
            TeamMemberCardView(
                it,
                profilePictureLookup,
                it.id != accountId,
            ) { message ->
                errorMessage = message
            }
        }

        // TODO Assigned Cases

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
    isPendingSync: Boolean = false,
    isSyncing: Boolean = false,
) {
    val t = LocalAppTranslator.current
    Column(
        // TODO Common colors
        Modifier.background(Color.White)
            .then(fillWidthPadded),
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

            // TODO Show if syncing or pending sync
        }

        val openCaseCount = team.openCaseCount
        Row(
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val caseCountTranslateKey =
                if (openCaseCount == 1) "~~{case_count} Open Case" else "~~{case_count} Open Cases"
            Text(
                t(caseCountTranslateKey)
                    .replace("{case_count}", "$openCaseCount"),
            )

            TeamCaseCompleteView(team)
        }
    }
}

@Composable
private fun TeamMemberCardView(
    person: PersonContact,
    profilePictureLookup: Map<Long, String>,
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
            verticalAlignment = Alignment.CenterVertically,
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
                // TODO List translated roles
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
                    if (hasPhone) {
                        TeamMemberOverflowMenu(contentSize) { context.openEmail(emailAddress) }
                    } else {
                        if (!context.openEmail(emailAddress)) {
                            onActionError(t("~~This device does not support sending emails."))
                        }
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
