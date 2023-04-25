package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.*
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.*
import com.crisiscleanup.feature.caseeditor.R
import kotlinx.coroutines.launch
import java.lang.Integer.min
import com.crisiscleanup.core.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseEditorRoute(
    onOpenExistingCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onEditPropertyData: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    onEditNotesFlags: () -> Unit = {},
    onEditDetails: () -> Unit = {},
    onEditWork: () -> Unit = {},
    onEditHazards: () -> Unit = {},
    onEditVolunteerReport: () -> Unit = {},
    onEditSearchAddress: () -> Unit = {},
    onEditMoveLocationOnMap: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {
    val editDifferentWorksite by viewModel.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        onOpenExistingCase(editDifferentWorksite)
    } else {
        val navigateBack by remember { viewModel.navigateBack }
        if (navigateBack) {
            onBackClick()
        } else {
            BackHandler {
                if (viewModel.onSystemBack()) {
                    onBackClick()
                }
            }

            val headerTitle by viewModel.headerTitle.collectAsStateWithLifecycle()
            val onNavigateBack = remember(viewModel) {
                {
                    if (viewModel.onNavigateBack()) {
                        onBackClick()
                    }
                }
            }
            val onNavigateCancel = remember(viewModel) {
                {
                    if (viewModel.onNavigateCancel()) {
                        onBackClick()
                    }
                }
            }
            Column {
                TopAppBarCancel(
                    title = headerTitle,
                    onCancel = onNavigateCancel,
                )
                CaseEditorScreen(
                    onNavigateBack = onNavigateBack,
                    onEditProperty = onEditPropertyData,
                    onEditLocation = onEditLocation,
                    onEditNotesFlags = onEditNotesFlags,
                    onEditDetails = onEditDetails,
                    onEditWork = onEditWork,
                    onEditHazards = onEditHazards,
                    onEditVolunteerReport = onEditVolunteerReport,
                    onEditSearchAddress = onEditSearchAddress,
                    onEditMoveLocationOnMap = onEditMoveLocationOnMap,
                )
            }
        }
    }
}

@Composable
internal fun ColumnScope.CaseEditorScreen(
    modifier: Modifier = Modifier,
    viewModel: CaseEditorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onEditProperty: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    onEditNotesFlags: () -> Unit = {},
    onEditDetails: () -> Unit = {},
    onEditWork: () -> Unit = {},
    onEditHazards: () -> Unit,
    onEditVolunteerReport: () -> Unit,
    onEditSearchAddress: () -> Unit = {},
    onEditMoveLocationOnMap: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState) {
        is CaseEditorUiState.Loading -> {
            Box(modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
        is CaseEditorUiState.WorksiteData -> {
            FullEditView(
                uiState as CaseEditorUiState.WorksiteData,
                onBack = onNavigateBack,
                onSearchAddress = onEditSearchAddress,
                onMoveLocation = onEditMoveLocationOnMap,
            )
        }
        else -> {
            val errorData = uiState as CaseEditorUiState.Error
            val errorMessage = if (errorData.errorResId != 0) stringResource(errorData.errorResId)
            else errorData.errorMessage.ifEmpty { stringResource(commonR.string.unexpected_error) }
            Box(modifier) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.FullEditView(
    worksiteData: CaseEditorUiState.WorksiteData,
    modifier: Modifier = Modifier,
    viewModel: CaseEditorViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
) {
    // TODO Pager should not affect or recompose content except when change in content to focus on should change

    val editSections by viewModel.editSections.collectAsStateWithLifecycle()

    var snapOnEndScroll by remember { mutableStateOf(false) }
    val rememberSnapOnEndScroll = remember(viewModel) { { snapOnEndScroll = true } }

    val pagerState = rememberLazyListState()
    SectionPager(
        editSections,
        modifier,
        rememberSnapOnEndScroll,
        pagerState,
    )

    var navigateToSectionIndex by remember { mutableStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val snapToIndex = if (pagerState.firstVisibleItemIndex >= editSections.size) {
                editSections.size - 1
            } else {
                pagerState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                    // TODO Account for (start) padding/spacing
                    if (it.offset < -it.size * 0.5) {
                        it.index + 1
                    } else {
                        it.index
                    }
                } ?: -1
            }

            if (snapToIndex >= 0) {
                val sectionIndex = min(snapToIndex, editSections.size - 1)
                if (snapOnEndScroll) {
                    snapOnEndScroll = false
                    coroutineScope.launch {
                        pagerState.animateScrollToItem(sectionIndex)
                    }
                } else {
                    navigateToSectionIndex = sectionIndex
                }
            }
        }
    }

    val isSavingData by viewModel.isSavingWorksite.collectAsStateWithLifecycle()
    val isEditable by remember(worksiteData, isSavingData) {
        derivedStateOf {
            worksiteData.isEditable && !isSavingData
        }
    }
    Box(Modifier.weight(1f)) {
        // TODO Why does content recompose when pager is scrolled?
        //      Optimize after all content is complete and eliminate unnecessary recompositions.
        //      Replace content with static text and it doesn't seem to recompose...
        FullEditContent(
            worksiteData,
            modifier,
            editSections,
            viewModel,
            isEditable,
            onMoveLocation = onMoveLocation,
            onSearchAddress = onSearchAddress,
        )

        val isLoadingWorksite by viewModel.isLoading.collectAsStateWithLifecycle()
        BusyIndicatorFloatingTopCenter(isLoadingWorksite)
    }

    val isDataChanged by viewModel.hasChanges.collectAsStateWithLifecycle()
    if (isDataChanged) {
        val saveChanges = remember(viewModel) { { viewModel.saveChanges() } }
        SaveActionBar(
            !isSavingData,
            onBack,
            saveChanges,
        )
    }

    val showBackChangesDialog by viewModel.promptUnsavedChanges
    val showCancelChangesDialog by viewModel.promptCancelChanges
    val abandonChanges = remember(viewModel) { { viewModel.abandonChanges() } }
    if (showBackChangesDialog) {
        val closeChangesDialog = { viewModel.promptUnsavedChanges.value = false }
        PromptChangesDialog(
            onStay = closeChangesDialog,
            onAbort = abandonChanges,
        )
    } else if (showCancelChangesDialog) {
        val closeChangesDialog = { viewModel.promptCancelChanges.value = false }
        PromptChangesDialog(
            onStay = closeChangesDialog,
            onAbort = abandonChanges,
        )
    }

    // TODO Prompt where required or inconsistent
//    InvalidSaveDialog(
//        onEditLocation = editLocation,
//        onEditPropertyData = editPropertyData,
//        onEditWork = editWork,
//    )
}

@Composable
private fun SectionPager(
    editSections: List<String>,
    modifier: Modifier = Modifier,
    snapToNearestIndex: () -> Unit = {},
    pagerState: LazyListState = rememberLazyListState(),
) {

    val pagerScrollConnection = remember(pagerState) {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                snapToNearestIndex()
                return super.onPostFling(consumed, available)
            }
        }
    }
    LazyRow(
        state = pagerState,
        modifier = Modifier.nestedScroll(pagerScrollConnection),
        contentPadding = listItemHorizontalPadding,
        horizontalArrangement = listItemSpacedBy,
    ) {
        items(editSections.size + 1) { index ->
            Box(
                modifier = modifier.listItemHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (index < editSections.size) {
                    val sectionTitle = editSections[index]
                    Text(
                        "${index + 1}. $sectionTitle",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                } else {
                    val endFillerItemWidth = LocalConfiguration.current.screenWidthDp.dp * 0.8f
                    Spacer(modifier = Modifier.width(endFillerItemWidth))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FullEditContent(
    worksiteData: CaseEditorUiState.WorksiteData,
    modifier: Modifier = Modifier,
    sectionTitles: List<String> = emptyList(),
    viewModel: CaseEditorViewModel = hiltViewModel(),
    isEditable: Boolean = false,
    openExistingCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
) {
    val closeKeyboard = rememberCloseKeyboard(viewModel)
    val scrollState = rememberScrollState()
    Column(
        modifier
            .scrollFlingListener(closeKeyboard)
            .verticalScroll(scrollState)
            .fillMaxSize()
    ) {
        val isLocalModified by remember { derivedStateOf { worksiteData.isLocalModified } }
        CaseIncident(
            modifier,
            worksiteData.incident.name,
            isLocalModified,
        )

        val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }

        val worksite by viewModel.editingWorksite.collectAsStateWithLifecycle()

        var isRerouted by remember { mutableStateOf(false) }

        if (sectionTitles.isNotEmpty()) {
            viewModel.propertyEditor?.let { propertyEditor ->
                PropertyLocationSection(
                    viewModel,
                    propertyEditor,
                    sectionTitles[0],
                    isEditable,
                    onMoveLocation,
                    onSearchAddress,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    sectionIndex: Int,
    sectionTitle: String,
    isCollapsed: Boolean = false,
    toggleCollapse: () -> Unit = {},
) {
    Row(
        modifier
            .clickable(onClick = toggleCollapse)
            .listItemHeight()
            .listItemPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedBy,
    ) {
        // TODO Bold
        val textStyle = MaterialTheme.typography.bodyLarge
        // TODO Can surface and box be combined into a single element?
        Surface(
            // TODO Common dimensions
            Modifier.size(26.dp),
            shape = CircleShape,
            color = attentionBackgroundColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "${sectionIndex + 1}",
                    style = textStyle,
                )
            }
        }
        Text(
            sectionTitle,
            Modifier.weight(1f),
            style = textStyle,
        )
        val iconVector =
            if (isCollapsed) CrisisCleanupIcons.ExpandLess else CrisisCleanupIcons.ExpandMore
        val descriptionResId =
            if (isCollapsed) R.string.collapse_section else R.string.expand_section
        val description = stringResource(descriptionResId, sectionTitle)
        Icon(
            imageVector = iconVector,
            contentDescription = description,
        )
    }
}

@Composable
private fun PropertyLocationSection(
    viewModel: CaseEditorViewModel,
    propertyEditor: CasePropertyDataEditor,
    sectionTitle: String,
    isEditable: Boolean,
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {}
) {
    var isPropertyCollapsed by remember { mutableStateOf(false) }
    val togglePropertySection =
        remember(viewModel) { { isPropertyCollapsed = !isPropertyCollapsed } }
    SectionHeader(
        sectionIndex = 0,
        sectionTitle = sectionTitle,
        isCollapsed = isPropertyCollapsed,
        toggleCollapse = togglePropertySection,
    )
    if (!isPropertyCollapsed) {
        PropertyFormView(
            viewModel,
            propertyEditor,
            isEditable,
        )

        viewModel.locationEditor?.let { locationEditor ->
            PropertyLocationView(
                viewModel,
                locationEditor,
                isEditable,
                onMoveLocationOnMap = onMoveLocation,
                openAddressSearch = onSearchAddress,
            )
        }

        viewModel.notesFlagsEditor?.let { notesFlagsEditor ->
            PropertyNotesFlagsView(
                viewModel,
                notesFlagsEditor,
                viewModel.visibleNoteCount,
            )
        }
    }
}

@Composable
private fun SectionSummaries(
    worksite: Worksite,
    isEditable: Boolean = true,
    translate: (String) -> String = { it },
    viewModel: CaseEditorViewModel = hiltViewModel(),
    editPropertyData: () -> Unit = {},
    editLocation: () -> Unit = {},
    editNotesFlags: () -> Unit = {},
    editDetails: () -> Unit = {},
    editWork: () -> Unit = {},
    editHazards: () -> Unit = {},
    editVolunteerReport: () -> Unit = {},
) {

    PropertySummaryView(
        worksite,
        isEditable,
        onEdit = editPropertyData,
        translate = translate,
    )

    LocationSummaryView(
        worksite,
        isEditable,
        onEdit = editLocation,
        translate = translate,
    )

    NotesFlagsSummaryView(
        worksite,
        isEditable,
        onEdit = editNotesFlags,
        collapsedNotesVisibleCount = viewModel.visibleNoteCount,
        translate = translate,
    )

    DetailsSummaryView(
        worksite,
        isEditable,
        onEdit = editDetails,
        translate = translate,
        summaryFieldLookup = viewModel.detailsFieldLookup,
    )

    val workTypeGroups by viewModel.worksiteWorkTypeGroups.collectAsStateWithLifecycle()
    val groupChildren by viewModel.workTypeGroupChildrenLookup.collectAsStateWithLifecycle()
    WorkSummaryView(
        worksite,
        isEditable,
        onEdit = editWork,
        translate = translate,
        workTypeGroups = workTypeGroups,
        groupChildren = groupChildren,
        summaryFieldLookup = viewModel.workFieldLookup,
    )

    HazardsSummaryView(
        worksite,
        isEditable,
        onEdit = editHazards,
        translate = translate,
        summaryFieldLookup = viewModel.hazardsFieldLookup,
    )

    VolunteerReportSummaryView(
        worksite,
        isEditable,
        onEdit = editVolunteerReport,
        translate = translate,
        summaryFieldLookup = viewModel.volunteerReportFieldLookup,
    )
}

@Composable
private fun CaseIncident(
    modifier: Modifier = Modifier,
    incidentName: String = "",
    isLocalModified: Boolean = false,
) {
    Row(
        modifier = modifier.listItemPadding(),
        verticalAlignment = Alignment.CenterVertically,
        // TODO Common dimensions
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            incidentName,
            style = MaterialTheme.typography.headlineMedium,
        )

        if (isLocalModified) {
            Icon(
                imageVector = CrisisCleanupIcons.CloudOff,
                contentDescription = stringResource(R.string.is_pending_sync),
            )
        }
    }
}

@Composable
private fun PromptChangesDialog(
    onStay: () -> Unit = {},
    onAbort: () -> Unit = {},
) {
    AlertDialog(
        title = { Text(stringResource(R.string.changes)) },
        text = { Text(stringResource(R.string.changes_choice)) },
        onDismissRequest = onStay,
        dismissButton = {
            CrisisCleanupTextButton(
                textResId = R.string.no,
                onClick = onAbort
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                textResId = R.string.yes,
                onClick = onStay,
            )
        },
    )
}

@Composable
private fun InvalidSaveDialog(
    onEditPropertyData: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    onEditWork: () -> Unit = {},
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {

    val promptInvalidSave by viewModel.showInvalidWorksiteSave.collectAsStateWithLifecycle()
    if (promptInvalidSave) {
        val invalidWorksiteInfo = viewModel.invalidWorksiteInfo.value
        if (invalidWorksiteInfo.invalidSection != WorksiteSection.None) {
            val messageResId =
                if (invalidWorksiteInfo.messageResId == 0) R.string.incomplete_worksite_data
                else invalidWorksiteInfo.messageResId
            val onDismiss =
                remember(viewModel) { { viewModel.showInvalidWorksiteSave.value = false } }
            AlertDialog(
                title = { Text(stringResource(R.string.incomplete_worksite)) },
                text = { Text(stringResource(messageResId)) },
                onDismissRequest = onDismiss,
                dismissButton = {
                    CrisisCleanupTextButton(
                        textResId = android.R.string.cancel,
                        onClick = onDismiss
                    )
                },
                confirmButton = {
                    CrisisCleanupTextButton(
                        textResId = R.string.fix,
                        onClick = {
                            when (invalidWorksiteInfo.invalidSection) {
                                WorksiteSection.Location -> onEditLocation()
                                WorksiteSection.Property -> onEditPropertyData()
                                WorksiteSection.WorkType -> onEditWork()
                                else -> {}
                            }
                            onDismiss()
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun SaveActionBar(
    enable: Boolean = true,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            // TODO Common dimensions
            .padding(16.dp),
        horizontalArrangement = listItemSpacedBy,
    ) {
        // TODO Use translations
        BusyButton(
            Modifier.weight(2f),
            textResId = R.string.back,
            enabled = enable,
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = cancelButtonContainerColor
            ),
        )
        BusyButton(
            Modifier.weight(3f),
            textResId = R.string.claim_and_save,
            enabled = enable,
            indicateBusy = !enable,
            onClick = onSave,
        )
        BusyButton(
            Modifier.weight(3f),
            textResId = R.string.save,
            enabled = enable,
            indicateBusy = !enable,
            onClick = onSave,
        )
    }
}

@Preview
@Composable
private fun CaseIncidentPreview() {
    Column {
        CaseIncident(
            incidentName = "Big sweeping hurricane across the gulf",
            isLocalModified = true,
        )
    }
}

@Preview
@Composable
private fun SaveActionBarPreview() {
    SaveActionBar()
}
