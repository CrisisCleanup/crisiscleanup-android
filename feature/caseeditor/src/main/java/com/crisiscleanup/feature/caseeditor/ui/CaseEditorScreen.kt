package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.*
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseEditorUiState
import com.crisiscleanup.feature.caseeditor.CaseEditorViewModel
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.WorksiteSection
import kotlinx.coroutines.launch
import java.lang.Integer.min
import com.crisiscleanup.core.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseEditorRoute(
    onEditPropertyData: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    onEditNotesFlags: () -> Unit = {},
    onEditDetails: () -> Unit = {},
    onEditWork: () -> Unit = {},
    onEditHazards: () -> Unit = {},
    onEditVolunteerReport: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {
    BackHandler {
        if (viewModel.onSystemBack()) {
            onBackClick()
        }
    }

    val navigateBack by remember { viewModel.navigateBack }
    if (navigateBack) {
        onBackClick()
    } else {
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
            TopAppBarBackCancel(
                title = headerTitle,
                onBack = onNavigateBack,
                onCancel = onNavigateCancel,
            )
            CaseEditorScreen(
                onEditProperty = onEditPropertyData,
                onEditLocation = onEditLocation,
                onEditNotesFlags = onEditNotesFlags,
                onEditDetails = onEditDetails,
                onEditWork = onEditWork,
                onEditHazards = onEditHazards,
                onEditVolunteerReport = onEditVolunteerReport,
            )
        }
    }
}

@Composable
internal fun ColumnScope.CaseEditorScreen(
    modifier: Modifier = Modifier,
    viewModel: CaseEditorViewModel = hiltViewModel(),
    onEditProperty: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    onEditNotesFlags: () -> Unit = {},
    onEditDetails: () -> Unit = {},
    onEditWork: () -> Unit = {},
    onEditHazards: () -> Unit,
    onEditVolunteerReport: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState) {
        is CaseEditorUiState.Loading -> {
            Box(modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
        is CaseEditorUiState.WorksiteData -> {
            FullEditView(uiState as CaseEditorUiState.WorksiteData)
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
) {
    // TODO Pager should not affect or recompose content except when change in content to focus on should change

    val editSections by viewModel.editSections.collectAsStateWithLifecycle()
    val pagerState = rememberLazyListState()
    var snapOnEndScroll by remember { mutableStateOf(false) }
    val rememberSnapOnEndScroll = remember(viewModel) { { snapOnEndScroll = true } }

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

    Text("Bottom")

//    ConstraintLayout(Modifier.fillMaxSize()) {
//        FullEditContent(
//            worksiteData,
//            modifier,
//            viewModel,
//            editPropertyData,
//            editLocation,
//            editNotesFlags,
//            editDetails,
//            editWork,
//            editHazards,
//            editVolunteerReport,
//        )
//    }
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
private fun ConstraintLayoutScope.FullEditContent(
    worksiteData: CaseEditorUiState.WorksiteData,
    modifier: Modifier = Modifier,
    viewModel: CaseEditorViewModel = hiltViewModel(),

    editPropertyData: () -> Unit = {},
    editLocation: () -> Unit = {},
    editWork: () -> Unit = {},
) {
    val (mainContent, busyIndicator, saveChangesRef) = createRefs()

    val isLoadingWorksite by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSavingData by viewModel.isSavingWorksite.collectAsStateWithLifecycle()
    val isEditable = worksiteData.isEditable && !isSavingData

    val isDataChanged by viewModel.hasChanges.collectAsStateWithLifecycle()

    val saveChanges = remember(viewModel) { { viewModel.saveChanges() } }
    var saveChangesButtonSize by remember { mutableStateOf(Size.Zero) }

    val closeKeyboard = rememberCloseKeyboard(viewModel)
    val scrollState = rememberScrollState()
    Column(
        modifier
            .constrainAs(mainContent) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
            .scrollFlingListener(closeKeyboard)
            .verticalScroll(scrollState)
    ) {
        CaseIncident(
            modifier,
            worksiteData.incident.name,
            worksiteData.isLocalModified,
        )

        val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }

        val worksite by viewModel.editingWorksite.collectAsStateWithLifecycle()

        if (isDataChanged) {
            Spacer(
                modifier = listItemModifier.height(
                    with(LocalDensity.current) { saveChangesButtonSize.height.toDp() }
                ),
            )
        }
    }

    AnimatedBusyIndicator(
        isBusy = isLoadingWorksite,
        modifier = Modifier.constrainAs(busyIndicator) {
            top.linkTo(parent.top)
            centerHorizontallyTo(parent)
        },
        // TODO Common dimensions
        padding = 48.dp
    )

    if (isDataChanged) {
        BusyButton(
            modifier = Modifier
                .constrainAs(saveChangesRef) {
                    bottom.linkTo(parent.bottom, margin = actionEdgeSpace)
                    end.linkTo(parent.end, margin = actionEdgeSpace)
                }
                .animateContentSize()
                .onGloballyPositioned {
                    saveChangesButtonSize = it.size.toSize()
                },
            textResId = R.string.save_changes,
            enabled = !isSavingData,
            indicateBusy = isSavingData,
            onClick = saveChanges,
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

    InvalidSaveDialog(
        onEditLocation = editLocation,
        onEditPropertyData = editPropertyData,
        onEditWork = editWork,
    )
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
