package com.crisiscleanup.feature.team.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
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
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.EquipmentData
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.UserRole
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.team.CreateEditTeamViewModel
import com.crisiscleanup.feature.team.EmptyTeamAssignableWorksite
import com.crisiscleanup.feature.team.MemberFilterResult
import com.crisiscleanup.feature.team.SinglePersonEquipment
import com.crisiscleanup.feature.team.TeamAssignableWorksite
import com.crisiscleanup.feature.team.TeamCaseMapManager
import kotlinx.coroutines.launch

@Composable
fun CreateEditTeamRoute(
    onBack: () -> Unit,
    hasCaseSearchResult: Boolean,
    takeSearchResult: () -> ExistingWorksiteIdentifier,
    onViewCase: (Long, Long) -> Unit,
    onSearchCases: () -> Unit,
    onFilterCases: () -> Unit,
    viewModel: CreateEditTeamViewModel = hiltViewModel(),
) {
    LaunchedEffect(hasCaseSearchResult) {
        val caseSearchResult = takeSearchResult()
        if (caseSearchResult != ExistingWorksiteIdentifierNone) {
            viewModel.onAssignCase(caseSearchResult)
        }
    }

    CreateEditTeamView(
        onBack,
        onViewCase = onViewCase,
        onSearchCases = onSearchCases,
        onFilterCases = onFilterCases,
    )
}

@Composable
private fun CreateEditTeamView(
    onBack: () -> Unit,
    onViewCase: (Long, Long) -> Unit,
    onSearchCases: () -> Unit,
    onFilterCases: () -> Unit,
    viewModel: CreateEditTeamViewModel = hiltViewModel(),
) {
    val tabState by viewModel.stepTabState.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isEditable = !isLoading

    val editingTeam by viewModel.editingTeam.collectAsStateWithLifecycle()

    val editingTeamMembers by viewModel.editingTeamMembers.collectAsStateWithLifecycle()
    val userRoleLookup by viewModel.userRoleLookup.collectAsStateWithLifecycle()

    val memberFilter by viewModel.teamMemberFilter.collectAsStateWithLifecycle()
    val membersState by viewModel.teamMembersState.collectAsStateWithLifecycle()

    val isLoadingSelectedMapCase by viewModel.isLoadingMapMarkerWorksite.collectAsStateWithLifecycle()
    val isAssigningCase by viewModel.isAssigningWorksite.collectAsStateWithLifecycle()
    val selectedMapCase by viewModel.selectedMapWorksite.collectAsStateWithLifecycle()
    val onMapCaseSelect = viewModel::onMapCaseMarkerSelect
    val assignedCases = viewModel.assignedWorksites

    val isCaseListView by viewModel.isCaseListView.collectAsStateWithLifecycle()

    var pagerIndex by remember { mutableIntStateOf(-1) }
    val updatePagerIndex = { page: Int ->
        pagerIndex = page
    }
    val caseListOnBack = remember(onBack, viewModel, isCaseListView, pagerIndex) {
        {
            if (isCaseListView && pagerIndex == tabState.casesTabIndex) {
                viewModel.toggleMapListView()
            } else {
                onBack()
            }
        }
    }
    BackHandler {
        caseListOnBack()
    }

    val equipmentOptions by viewModel.equipmentOptions.collectAsStateWithLifecycle()
    val memberOptions by viewModel.memberOptions.collectAsStateWithLifecycle()
    val teamEquipment by viewModel.teamEquipment.collectAsStateWithLifecycle()

    Column {
        TeamEditorHeader(
            title = viewModel.headerTitle,
            subTitle = viewModel.headerSubTitle,
            onCancel = caseListOnBack,
        )

        Box(Modifier.fillMaxSize()) {
            if (tabState.titles.isNotEmpty()) {
                CreateEditTeamPager(
                    isLoading,
                    tabState.titles,
                    editingTeam,
                    initialPageTabIndex = tabState.startingIndex,
                    onPageChange = updatePagerIndex,
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
                    memberFilter = memberFilter,
                    onUpdateMemberFilter = viewModel::onUpdateTeamMemberFilter,
                    caseMapManager = viewModel.caseMapManager,
                    onMapCaseSelect = onMapCaseSelect,
                    isLoadingSelectedMapCase = isLoadingSelectedMapCase,
                    isAssigningCase = isAssigningCase,
                    selectedMapCase = selectedMapCase,
                    onViewCase = onViewCase,
                    onAssignCase = viewModel::onAssignCase,
                    onClearSelectedMapCase = viewModel::clearSelectedMapCase,
                    assignedCases = assignedCases,
                    onFilterCases = onFilterCases,
                    onSearchCases = onSearchCases,
                    iconProvider = viewModel.mapCaseIconProvider,
                    isCaseListView = isCaseListView,
                    onToggleCaseListView = viewModel::toggleMapListView,
                    onUnassignCase = viewModel::onUnassignCase,
                    equipmentOptions = equipmentOptions,
                    memberOptions = memberOptions,
                    teamEquipment = teamEquipment,
                )
            }

            BusyIndicatorFloatingTopCenter(isLoading)
        }
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

@Composable
private fun CreateEditTeamPager(
    isLoading: Boolean,
    tabTitles: List<String>,
    team: CleanupTeam,
    initialPageTabIndex: Int,
    onPageChange: (Int) -> Unit,
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
    memberFilter: String,
    onUpdateMemberFilter: (String) -> Unit,
    caseMapManager: TeamCaseMapManager,
    onMapCaseSelect: (WorksiteMapMark) -> Unit = {},
    isLoadingSelectedMapCase: Boolean = false,
    isAssigningCase: Boolean = false,
    selectedMapCase: TeamAssignableWorksite = EmptyTeamAssignableWorksite,
    iconProvider: MapCaseIconProvider,
    onViewCase: (Long, Long) -> Unit = { _, _ -> },
    onAssignCase: (ExistingWorksiteIdentifier) -> Unit = {},
    assignedCases: List<Worksite> = emptyList(),
    onClearSelectedMapCase: () -> Unit = {},
    onSearchCases: () -> Unit = {},
    onFilterCases: () -> Unit = {},
    isCaseListView: Boolean = false,
    onToggleCaseListView: () -> Unit = {},
    onUnassignCase: (Worksite) -> Unit = {},
    equipmentOptions: List<EquipmentData> = emptyList(),
    memberOptions: List<PersonContact> = emptyList(),
    teamEquipment: List<SinglePersonEquipment> = emptyList(),
) {
    // TODO Page does not keep across first orientation change
    val pagerState = rememberPagerState(
        initialPage = initialPageTabIndex,
        initialPageOffsetFraction = 0f,
    ) { tabTitles.size }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChange(page)
        }
    }
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
                    memberFilter,
                    onUpdateMemberFilter,
                )

                2 -> {
                    val openCase = remember(onViewCase) {
                        { worksite: Worksite ->
                            with(worksite) {
                                onViewCase(incidentId, id)
                            }
                        }
                    }
                    val openSelectedCase = remember(openCase, selectedMapCase) {
                        {
                            openCase(selectedMapCase.worksite)
                        }
                    }
                    val assignCase = remember(selectedMapCase, onAssignCase) {
                        {
                            onAssignCase(
                                with(selectedMapCase.worksite) {
                                    ExistingWorksiteIdentifier(
                                        incidentId = incidentId,
                                        worksiteId = id,
                                    )
                                },
                            )
                        }
                    }
                    val unassignCase = remember(selectedMapCase, onUnassignCase) {
                        {
                            onUnassignCase(selectedMapCase.worksite)
                        }
                    }

                    Column(Modifier.animateContentSize()) {
                        EditTeamCasesView(
                            isLoading,
                            assignedCases,
                            isLoadingSelectedMapCase = isLoadingSelectedMapCase,
                            isListView = isCaseListView,
                            // TODO
                            isLayerView = false,
                            isAssigningCase = isAssigningCase,
                            caseMapManager,
                            iconProvider,
                            Modifier.weight(1f),
                            onMapCaseSelect = onMapCaseSelect,
                            onPropagateTouchScroll = setEnablePagerScroll,
                            onSearchCases = onSearchCases,
                            onFilterCases = onFilterCases,
                            toggleMapListView = onToggleCaseListView,
                            onViewCase = openCase,
                            onUnassignCase = onUnassignCase,
                        )

                        EditTeamMapCaseOverview(
                            isAssigningCase,
                            selectedMapCase,
                            iconProvider,
                            onViewDetails = openSelectedCase,
                            onAssignCase = assignCase,
                            onClearSelection = onClearSelectedMapCase,
                            onUnassignCase = unassignCase,
                        )
                    }
                }

                3 -> EditTeamEquipmentView(
                    equipmentOptions,
                    memberOptions,
                    teamEquipment,
                )

                4 -> ReviewChangesView(team)
            }
        }
    }

    val closeKeyboard = rememberCloseKeyboard()
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

    val closeKeyboard = rememberCloseKeyboard()

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
private fun ReviewChangesView(
    team: CleanupTeam,
) {
    Text("Review team changes")
    // TODO Reuse views from team summary
}
