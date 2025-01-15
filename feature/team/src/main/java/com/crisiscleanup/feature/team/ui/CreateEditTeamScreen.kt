package com.crisiscleanup.feature.team.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.ui.CaseAddressInfoView
import com.crisiscleanup.core.commoncase.ui.CaseMapOverlayElements
import com.crisiscleanup.core.commoncase.ui.CasePhoneInfoView
import com.crisiscleanup.core.commoncase.ui.CasesAction
import com.crisiscleanup.core.commoncase.ui.CasesDownloadProgress
import com.crisiscleanup.core.commoncase.ui.CasesMapView
import com.crisiscleanup.core.commoncase.ui.CrisisCleanupFab
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.HeaderSubTitle
import com.crisiscleanup.core.designsystem.component.HeaderTitle
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.SmallBusyIndicator
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.component.actionSize
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.edgePadding
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EmptyCleanupTeam
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.UserRole
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.touchDownConsumer
import com.crisiscleanup.feature.team.CreateEditTeamViewModel
import com.crisiscleanup.feature.team.EmptyTeamAssignableWorksite
import com.crisiscleanup.feature.team.MemberFilterResult
import com.crisiscleanup.feature.team.TeamAssignableWorksite
import com.crisiscleanup.feature.team.TeamCaseMapManager
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.rememberTileOverlayState
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

                3 -> EditTeamEquipmentView(team)
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
private fun EditTeamCasesView(
    isLoading: Boolean,
    assignedCases: List<Worksite>,
    isLoadingSelectedMapCase: Boolean,
    isListView: Boolean,
    isAssigningCase: Boolean,
    mapManager: TeamCaseMapManager,
    iconProvider: MapCaseIconProvider,
    modifier: Modifier = Modifier,
    onMapCaseSelect: (WorksiteMapMark) -> Unit = { },
    onPropagateTouchScroll: (Boolean) -> Unit = {},
    onSearchCases: () -> Unit = {},
    onFilterCases: () -> Unit = {},
    toggleMapListView: () -> Unit = {},
    onViewCase: (Worksite) -> Unit = {},
    onUnassignCase: (Worksite) -> Unit = {},
) {
    val mapModifier = remember(onPropagateTouchScroll) {
        Modifier.touchDownConsumer { onPropagateTouchScroll(false) }
    }

    val onCasesAction = remember(mapManager, onFilterCases, toggleMapListView) {
        { action: CasesAction ->
            when (action) {
                CasesAction.Layers -> mapManager.toggleLayersView()
                CasesAction.ZoomToInteractive -> mapManager.zoomToInteractive()
                CasesAction.ZoomToIncident -> mapManager.zoomToIncidentBounds()
                CasesAction.ZoomIn -> mapManager.zoomIn()
                CasesAction.ZoomOut -> mapManager.zoomOut()
                CasesAction.Search -> onSearchCases()
                CasesAction.Filters -> onFilterCases()
                CasesAction.ListView -> toggleMapListView()
                else -> mapManager.onCasesAction(action)
            }
        }
    }

    val filtersCount by mapManager.filtersCount.collectAsStateWithLifecycle(0)
    val isMapBusy by mapManager.isMapBusy.collectAsStateWithLifecycle(false)
    val casesCountMapText by mapManager.casesCountMapText.collectAsStateWithLifecycle()
    val worksitesOnMap by mapManager.worksitesMapMarkers.collectAsStateWithLifecycle()
    val mapCameraBounds by mapManager.incidentLocationBounds.collectAsStateWithLifecycle()
    val mapCameraZoom by mapManager.mapCameraZoom.collectAsStateWithLifecycle()
    val tileOverlayState = rememberTileOverlayState()
    val tileChangeValue by mapManager.overviewTileDataChange
    val clearTileLayer = remember(mapManager) { { mapManager.clearTileLayer } }
    val onMapCameraChange = remember(mapManager, onPropagateTouchScroll) {
        { position: CameraPosition, projection: Projection?, isActiveMove: Boolean ->
            if (!isActiveMove) {
                onPropagateTouchScroll(true)
            }
            mapManager.onMapCameraChange(position, projection, isActiveMove)
        }
    }
    val dataProgress by mapManager.dataProgress.collectAsStateWithLifecycle()
    val isLoadingData by mapManager.isLoadingData.collectAsStateWithLifecycle(true)
    val onMapMarkerSelect = remember(mapManager) {
        { mark: WorksiteMapMark ->
            onMapCaseSelect(mark)
            true
        }
    }
    val isMyLocationEnabled by mapManager.isMyLocationEnabled.collectAsStateWithLifecycle()

    val onSyncDataDelta = remember(mapManager) {
        {
            mapManager.syncWorksitesDelta(false)
        }
    }
    val onSyncDataFull = remember(mapManager) {
        {
            mapManager.syncWorksitesDelta(true)
        }
    }

    if (isListView) {
        Box(modifier) {
            AssignedCasesView(
                isLoadingCases = isLoading,
                assignedCases,
                iconProvider,
                Modifier.fillMaxSize(),
                onSearchCases = onSearchCases,
                onViewCase = onViewCase,
                onUnassignCase = onUnassignCase,
            )

            CrisisCleanupFab(
                CasesAction.MapView,
                enabled = true,
                Modifier.align(Alignment.BottomEnd)
                    .edgePadding()
                    .actionSize(),
                onClick = toggleMapListView,
            )
        }
    } else {
        Box(modifier) {
            CasesMapView(
                modifier = mapModifier,
                mapCameraBounds,
                mapCameraZoom,
                isMapBusy,
                worksitesOnMap,
                tileChangeValue,
                clearTileLayer,
                tileOverlayState,
                mapManager::overviewMapTileProvider,
                mapManager::onMapLoadStart,
                mapManager::onMapLoaded,
                onMapCameraChange,
                onMapMarkerSelect,
                null,
                isMyLocationEnabled,
            )

            // TODO Use MapOverlayMessage
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = (-24).dp)
                    .padding(LocalDimensions.current.edgePadding)
                    // TODO Common dimensions
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.disabledAlpha())
                    .padding(8.dp),
                horizontalArrangement = listItemSpacedByHalf,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    LocalAppTranslator.current("~~Select Cases to assign to team"),
                )

                AnimatedVisibility(
                    isLoadingSelectedMapCase || isAssigningCase,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SmallBusyIndicator(padding = 0.dp)
                }
            }

            CaseMapOverlayElements(
                Modifier,
                onCasesAction = onCasesAction,
                centerOnMyLocation = mapManager::grantAccessToDeviceLocation,
                isLoadingData = isLoadingData,
                casesCountText = casesCountMapText,
                filtersCount = filtersCount,
                disableTableViewActions = true,
                onSyncDataDelta = onSyncDataDelta,
                onSyncDataFull = onSyncDataFull,
                showCasesMainActions = false,
                assignedCaseCount = assignedCases.size,
            )

            CasesDownloadProgress(dataProgress)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTeamMapCaseOverview(
    isAssigningCase: Boolean,
    selectedWorksite: TeamAssignableWorksite,
    iconProvider: MapCaseIconProvider,
    onViewDetails: () -> Unit = {},
    onAssignCase: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onUnassignCase: () -> Unit = {},
) {
    val t = LocalAppTranslator.current

    if (selectedWorksite != EmptyTeamAssignableWorksite) {
        ModalBottomSheet(
            onDismissRequest = onClearSelection,
            tonalElevation = 0.dp,
        ) {
            Column(verticalArrangement = listItemSpacedByHalf) {
                Row(
                    listItemModifier,
                    horizontalArrangement = listItemSpacedBy,
                    verticalAlignment = Alignment.Top,
                ) {
                    selectedWorksite.worksite.keyWorkType?.let { keyWorkType ->
                        iconProvider.getIconBitmap(
                            keyWorkType.statusClaim,
                            keyWorkType.workType,
                            hasMultipleWorkTypes = selectedWorksite.worksite.workTypes.size > 1,
                        )?.let { bitmap ->
                            // TODO Review if this produces the intended description
                            val workTypeLiteral =
                                t(selectedWorksite.worksite.keyWorkType?.workTypeLiteral ?: "")
                            Image(
                                bitmap.asImageBitmap(),
                                contentDescription = workTypeLiteral,
                            )
                        }
                    }

                    with(selectedWorksite.worksite) {
                        Column(verticalArrangement = listItemSpacedByHalf) {
                            Text(
                                "$name, $caseNumber",
                                Modifier.listItemTopPadding(),
                                fontWeight = FontWeight.Bold,
                            )

                            CaseAddressInfoView(
                                this@with,
                                false,
                                Modifier.listItemVerticalPadding(),
                            )

                            CasePhoneInfoView(
                                this@with,
                                false,
                                Modifier.listItemVerticalPadding(),
                            )
                        }
                    }
                }

                Row(
                    fillWidthPadded,
                    horizontalArrangement = listItemSpacedBy,
                ) {
                    CrisisCleanupOutlinedButton(
                        modifier = Modifier
                            .actionHeight()
                            .weight(1f),
                        text = t("~~View Details"),
                        onClick = onViewDetails,
                        enabled = !isAssigningCase,
                    )

                    if (selectedWorksite.isAssigned) {
                        CrisisCleanupButton(
                            modifier = Modifier.weight(1f),
                            text = t("~~Unassign Case"),
                            onClick = onUnassignCase,
                        )
                    } else {
                        BusyButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isAssigningCase && selectedWorksite.isAssignable,
                            text = t("~~Assign Case"),
                            indicateBusy = isAssigningCase,
                            onClick = onAssignCase,
                        )
                    }
                }
            }
        }
    }
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
