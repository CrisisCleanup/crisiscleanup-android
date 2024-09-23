package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.HeaderSubTitle
import com.crisiscleanup.core.designsystem.component.HeaderTitle
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.UserRole
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.team.CreateEditTeamViewModel
import com.crisiscleanup.feature.team.MemberFilterResult
import kotlinx.coroutines.launch

@Composable
fun CreateEditTeamRoute(
    onBack: () -> Unit,
) {
    CreateEditTeamView(onBack)
}

@Composable
private fun CreateEditTeamView(
    onBack: () -> Unit,
    viewModel: CreateEditTeamViewModel = hiltViewModel(),
) {
    val tabState by viewModel.stepTabState.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isEditable = !isLoading

    val editingTeam by viewModel.editingTeam.collectAsStateWithLifecycle()

    val editingTeamMembers by viewModel.editingTeamMembers.collectAsStateWithLifecycle()
    val userRoleLookup by viewModel.userRoleLookup.collectAsStateWithLifecycle()

    var showJoinTeamQrCode by rememberSaveable { mutableStateOf(false) }

    val memberFilter by viewModel.teamMemberFilter.collectAsStateWithLifecycle()
    val membersState by viewModel.teamMembersState.collectAsStateWithLifecycle()

    Column {
        TeamEditorHeader(
            title = viewModel.headerTitle,
            subTitle = viewModel.headerSubTitle,
            onCancel = onBack,
        )

        Box(Modifier.fillMaxSize()) {
            if (tabState.titles.isNotEmpty()) {
                CreateEditTeamContent(
                    tabState.titles,
                    editingTeam,
                    tabState.startingIndex,
                    isEditable = isEditable,
                    teamName = viewModel.editingTeamName,
                    teamNotes = viewModel.editingTeamNotes,
                    teamMembers = editingTeamMembers,
                    membersState = membersState,
                    onTeamNameChange = viewModel::onTeamNameChange,
                    onTeamNotesChange = viewModel::onTeamNotesChange,
                    onSuggestName = viewModel::onSuggestTeamName,
                    onRemoveTeamMember = viewModel::onRemoveTeamMember,
                    onAddTeamMember = viewModel::onAddTeamMember,
                    userRoleLookup = userRoleLookup,
                    onToggleJoinTeamQrCode = { showJoinTeamQrCode = !showJoinTeamQrCode },
                    memberFilter = memberFilter,
                    onUpdateMemberFilter = viewModel::onUpdateTeamMemberFilter,
                )
            }

            BusyIndicatorFloatingTopCenter(isLoading)
        }
    }

    if (showJoinTeamQrCode) {
        val hideJoinTeamQrCode = { { showJoinTeamQrCode = false } }
        // TODO QR code UI
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TeamEditorHeader(
    title: String,
    subTitle: String = "",
    onCancel: () -> Unit = {},
) {
    val titleContent = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderTitle(
                title,
                Modifier.testTag("teamEditorHeaderTitle"),
            )
            HeaderSubTitle(
                subTitle,
                Modifier.testTag("teamEditorHeaderSubTitle"),
            )
        }
    }

    val navigationContent = @Composable { TopBarBackAction(onCancel) }

    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreateEditTeamContent(
    tabTitles: List<String>,
    team: CleanupTeam,
    initialPage: Int,
    isEditable: Boolean,
    teamName: String,
    teamNotes: String,
    teamMembers: List<PersonContact>,
    membersState: MemberFilterResult,
    onTeamNameChange: (String) -> Unit,
    onSuggestName: () -> Unit,
    onTeamNotesChange: (String) -> Unit,
    onRemoveTeamMember: (PersonContact) -> Unit,
    onAddTeamMember: (PersonContact) -> Unit,
    userRoleLookup: Map<Int, UserRole>,
    onToggleJoinTeamQrCode: () -> Unit = {},
    memberFilter: String = "",
    onUpdateMemberFilter: (String) -> Unit = {},
) {
    // TODO Page does not keep across first orientation change
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f,
    ) { tabTitles.size }
    val selectedTabIndex = pagerState.currentPage
    val coroutine = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            indicator = @Composable { tabPositions ->
                SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = LocalDimensions.current.tabIndicatorHeight,
                    color = primaryOrangeColor,
                )
            },
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    text = {
                        Text(
                            title,
                            style = LocalFontStyles.current.header4,
                        )
                    },
                    selected = selectedTabIndex == index,
                    onClick = {
                        coroutine.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier.testTag("caseInfoTab_$title"),
                )
            }
        }

        var enablePagerScroll by remember { mutableStateOf(true) }
        val setEnablePagerScroll = remember(pagerState) { { b: Boolean -> enablePagerScroll = b } }

        HorizontalPager(
            pagerState,
            Modifier.fillMaxSize(),
            userScrollEnabled = enablePagerScroll,
        ) { pagerIndex ->
            when (pagerIndex) {
                0 -> EditTeamInfoView(
                    team.colorInt,
                    teamName,
                    isEditable = isEditable,
                    hasFocus = team.id == EmptyCleanupTeam.id,
                    onTeamNameChange,
                    onSuggestName,
                    teamNotes,
                    onTeamNotesChange,
                )

                1 -> EditTeamMembersView(
                    teamMembers,
                    membersState,
                    onRemoveMember = onRemoveTeamMember,
                    onAddMember = onAddTeamMember,
                    isEditable,
                    userRoleLookup,
                    onToggleJoinTeamQrCode,
                    memberFilter,
                    onUpdateMemberFilter,
                )

                2 -> EditTeamCasesView(team)
                3 -> EditTeamEquipmentView(team)
                4 -> ReviewChangesView(team)
            }
        }
    }

    val closeKeyboard = rememberCloseKeyboard(pagerState)
    val pagerPage by remember(pagerState) {
        derivedStateOf {
            pagerState.currentPage
        }
    }
    LaunchedEffect(pagerPage) {
        closeKeyboard()
    }
}

@Composable
private fun EditTeamInfoView(
    teamColorInt: Int,
    name: String,
    isEditable: Boolean,
    hasFocus: Boolean,
    onTeamNameChange: (String) -> Unit,
    onSuggestName: () -> Unit,
    notes: String,
    onTeamNotesChange: (String) -> Unit,
) {
    val t = LocalAppTranslator.current

    val closeKeyboard = rememberCloseKeyboard(onTeamNameChange)

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = listItemSpacedByHalf,
    ) {
        Box(listItemModifier.listItemTopPadding()) {
            TeamColorView(
                teamColorInt,
            )
        }

        OutlinedClearableTextField(
            modifier = listItemModifier
                .testTag("teamEditorNameTextField"),
            label = t("~~Team name"),
            value = name,
            onValueChange = { onTeamNameChange(it) },
            hasFocus = hasFocus,
            keyboardType = KeyboardType.Password,
            enabled = isEditable,
            isError = false,
            keyboardCapitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        )

        val color = if (isEditable) {
            primaryBlueColor
        } else {
            primaryBlueColor.disabledAlpha()
        }
        Box(
            Modifier
                .clickable(
                    onClick = onSuggestName,
                )
                .listItemPadding()
                .actionHeight()
                .align(Alignment.End)
                .testTag("teamEditorSuggestNameAction"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                t("~~Suggest a name"),
                color = color,
                style = LocalFontStyles.current.header3,
            )
        }

        CrisisCleanupTextArea(
            text = notes,
            onTextChange = onTeamNotesChange,
            modifier = listItemModifier,
            label = { Text(t("~~Notes")) },
            imeAction = ImeAction.Done,
            enabled = isEditable,
            onDone = closeKeyboard,
        )

        Modifier.weight(1f)
    }
}

@Composable
private fun EditTeamCasesView(
    team: CleanupTeam,
) {
    Text("Edit cases ${team.metrics.caseCount}")
}

@Composable
private fun EditTeamEquipmentView(
    team: CleanupTeam,
) {
    Text("Edit equipment ${team.equipment.size}")
}

@Composable
private fun ReviewChangesView(
    team: CleanupTeam,
) {
    Text("Review team changes")
    // TODO Reuse views from team summary
}
