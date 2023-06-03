package com.crisiscleanup.feature.caseeditor.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContainerColor
import com.crisiscleanup.core.designsystem.theme.incidentDisasterContentColor
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.separatorColor
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseEditorUiState
import com.crisiscleanup.feature.caseeditor.CaseEditorViewModel
import com.crisiscleanup.feature.caseeditor.CasePropertyDataEditor
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.WorksiteSection
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import kotlinx.coroutines.launch
import com.crisiscleanup.core.common.R as commonR

private const val SectionHeaderContentType = "section-header-content-type"
private const val SectionSeparatorContentType = "section-header-content-type"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseEditorRoute(
    changeNewIncidentCase: (Long) -> Unit = {},
    onOpenExistingCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onEditSearchAddress: () -> Unit = {},
    onEditMoveLocationOnMap: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {
    val changeWorksiteIncidentId by viewModel.changeWorksiteIncidentId.collectAsStateWithLifecycle()
    val editDifferentWorksite by viewModel.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        onOpenExistingCase(editDifferentWorksite)
    } else if (changeWorksiteIncidentId != EmptyIncident.id) {
        changeNewIncidentCase(changeWorksiteIncidentId)
    } else {
        val navigateBack by remember { viewModel.navigateBack }
        if (navigateBack) {
            onBack()
        } else {
            BackHandler {
                if (viewModel.onSystemBack()) {
                    onBack()
                }
            }

            val headerTitle by viewModel.headerTitle.collectAsStateWithLifecycle()
            val onNavigateBack = remember(viewModel) {
                {
                    if (viewModel.onNavigateBack()) {
                        onBack()
                    }
                }
            }
            val onNavigateCancel = remember(viewModel) {
                {
                    if (viewModel.onNavigateCancel()) {
                        onBack()
                    }
                }
            }
            Column(Modifier.background(color = Color.White)) {
                TopAppBarBackAction(
                    title = headerTitle,
                    onAction = onNavigateBack,
                )
                CaseEditorScreen(
                    onNavigateCancel = onNavigateCancel,
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
    onNavigateCancel: () -> Unit = {},
    onEditSearchAddress: () -> Unit = {},
    onEditMoveLocationOnMap: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState) {
        is CaseEditorUiState.Loading -> {
            Box(
                modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                BusyIndicatorFloatingTopCenter(true)
            }
        }

        is CaseEditorUiState.CaseData -> {
            FullEditView(
                uiState as CaseEditorUiState.CaseData,
                onCancel = onNavigateCancel,
                onSearchAddress = onEditSearchAddress,
                onMoveLocation = onEditMoveLocationOnMap,
            )
        }

        else -> {
            val errorData = uiState as CaseEditorUiState.Error
            val errorMessage = if (errorData.errorResId != 0) stringResource(errorData.errorResId)
            else errorData.errorMessage.ifEmpty { stringResource(commonR.string.unexpected_error) }
            Box(modifier.weight(1f)) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun OnSliderScrollRest(
    sectionCount: Int,
    sliderState: LazyListState,
    onScrollRest: (Int) -> Unit,
) {
    LaunchedEffect(sliderState.isScrollInProgress) {
        if (!sliderState.isScrollInProgress) {
            val snapToIndex = if (sliderState.firstVisibleItemIndex >= sectionCount) {
                sectionCount - 1
            } else {
                sliderState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                    // TODO Account for (start) padding/spacing
                    if (it.offset < -it.size * 0.5) {
                        it.index + 1
                    } else {
                        it.index
                    }
                } ?: -1
            }

            if (snapToIndex >= 0) {
                val sectionIndex = snapToIndex.coerceAtMost(sectionCount - 1)
                onScrollRest(sectionIndex)
            }
        }
    }
}

@Composable
private fun OnContentScrollRest(
    contentListState: LazyListState,
    indexLookups: SectionContentIndexLookup,
    sectionCollapseStates: SnapshotStateList<Boolean>,
    takeScrollToSection: () -> Boolean = { false },
    onScrollRest: (Int) -> Unit,
) {
    LaunchedEffect(contentListState.isScrollInProgress) {
        if (!contentListState.isScrollInProgress && takeScrollToSection()) {
            var actualItemIndex = contentListState.firstVisibleItemIndex
            sectionCollapseStates.forEachIndexed { index, isCollapsed ->
                indexLookups.sectionItem[index]?.let { sectionItemIndex ->
                    if (isCollapsed) {
                        indexLookups.sectionItemCount[index]?.let { sectionItemCount ->
                            actualItemIndex += sectionItemCount
                        }
                    }
                    if (actualItemIndex >= sectionItemIndex) {
                        return@forEachIndexed
                    }
                } ?: return@forEachIndexed
            }
            val sliderIndex = if (actualItemIndex == 0) {
                0
            } else if (actualItemIndex < indexLookups.maxItemIndex) {
                indexLookups.itemSection[actualItemIndex] ?: -1
            } else {
                indexLookups.maxSectionIndex
            }
            if (sliderIndex >= 0 && sliderIndex <= indexLookups.maxSectionIndex) {
                onScrollRest(sliderIndex)
            }
        }
    }
}

@Composable
private fun ColumnScope.FullEditView(
    caseData: CaseEditorUiState.CaseData,
    modifier: Modifier = Modifier,
    viewModel: CaseEditorViewModel = hiltViewModel(),
    onCancel: () -> Unit = {},
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
) {
    val editSections by viewModel.editSections.collectAsStateWithLifecycle()
    val sectionCollapseStates = remember(viewModel) {
        val collapseStates = SnapshotStateList<Boolean>()
        for (i in editSections) {
            collapseStates.add(false)
        }
        collapseStates
    }

    var snapOnEndScroll by remember { mutableStateOf(false) }
    val rememberSnapOnEndScroll = remember(viewModel) { { snapOnEndScroll = true } }

    val pagerState = rememberLazyListState()

    val indexLookups by rememberSectionContentIndexLookup(
        mapOf(
            0 to 1,
            1 to 6,
            2 to 9,
            3 to 12,
            4 to 15,
        )
    )
    val contentListState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()
    var isSliderScrollToSection by remember { mutableStateOf(false) }
    val sliderScrollToSectionItem = remember(viewModel) {
        { sectionIndex: Int, itemIndex: Int ->
            if (sectionIndex >= 0 && sectionIndex < sectionCollapseStates.size) {
                coroutineScope.launch {
                    isSliderScrollToSection = true

                    if (sectionCollapseStates[sectionIndex]) {
                        sectionCollapseStates[sectionIndex] = false
                    }

                    var visibleItemIndex = itemIndex
                    for (i in (sectionIndex - 1) downTo 0) {
                        if (sectionCollapseStates[i]) {
                            indexLookups.sectionItemCount[i]?.let { sectionItemCount ->
                                visibleItemIndex -= sectionItemCount
                            }
                        }
                    }

                    pagerState.animateScrollToItem(sectionIndex)
                    contentListState.animateScrollToItem(visibleItemIndex.coerceAtLeast(0))
                }
            }
        }
    }
    val sliderScrollToSection = remember(viewModel) {
        { index: Int ->
            indexLookups.sectionItem[index]?.let { itemIndex ->
                sliderScrollToSectionItem(index, itemIndex)
            }
            Unit
        }
    }

    // TODO Animate elevation when content scrolls below
    SectionPager(
        editSections,
        modifier,
        rememberSnapOnEndScroll,
        pagerState,
        sliderScrollToSection,
    )

    val onSliderScrollRest = remember(pagerState) {
        { sectionIndex: Int ->
            if (snapOnEndScroll) {
                snapOnEndScroll = false
                sliderScrollToSection(sectionIndex)
            }
        }
    }
    OnSliderScrollRest(editSections.size, pagerState, onSliderScrollRest)

    val takeScrollToSection = remember(contentListState) {
        {
            if (isSliderScrollToSection) {
                isSliderScrollToSection = false
                false
            } else {
                true
            }
        }
    }
    val onContentScrollRest = remember(contentListState) {
        { sliderIndex: Int ->
            if (sliderIndex != pagerState.firstVisibleItemIndex) {
                coroutineScope.launch {
                    pagerState.animateScrollToItem(sliderIndex)
                }
            }
        }
    }
    OnContentScrollRest(
        contentListState,
        indexLookups,
        sectionCollapseStates,
        takeScrollToSection,
        onContentScrollRest,
    )

    val areEditorsReady by viewModel.areEditorsReady.collectAsStateWithLifecycle()
    val isSavingData by viewModel.isSavingWorksite.collectAsStateWithLifecycle()
    val isEditable = areEditorsReady &&
            caseData.isNetworkLoadFinished &&
            !isSavingData

    val isSectionCollapsed =
        remember(viewModel) { { sectionIndex: Int -> sectionCollapseStates[sectionIndex] } }
    val toggleSectionCollapse = remember(viewModel) {
        { sectionIndex: Int ->
            sectionCollapseStates[sectionIndex] = !sectionCollapseStates[sectionIndex]
        }
    }
    val togglePropertySection = remember(viewModel) { { toggleSectionCollapse(0) } }

    val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }

    Box(Modifier.weight(1f)) {
        val closeKeyboard = rememberCloseKeyboard(viewModel)

        val caseEditor = CaseEditor(isEditable, caseData.statusOptions)
        CompositionLocalProvider(LocalCaseEditor provides caseEditor) {
            LazyColumn(
                modifier
                    .scrollFlingListener(closeKeyboard)
                    .fillMaxSize(),
                state = contentListState,
            ) {
                fullEditContent(
                    caseData,
                    viewModel,
                    modifier,
                    editSections,
                    onMoveLocation = onMoveLocation,
                    onSearchAddress = onSearchAddress,
                    isPropertySectionCollapsed = sectionCollapseStates[0],
                    togglePropertySection = togglePropertySection,
                    isSectionCollapsed = isSectionCollapsed,
                    toggleSection = toggleSectionCollapse,
                    translate = translate,
                )
            }
        }

        val isLoadingWorksite by viewModel.isLoading.collectAsStateWithLifecycle()
        BusyIndicatorFloatingTopCenter(isLoadingWorksite)
    }

    val claimAndSaveChanges = remember(viewModel) { { viewModel.saveChanges(true) } }
    val saveChanges = remember(viewModel) { { viewModel.saveChanges(false) } }
    SaveActionBar(
        !isSavingData,
        onCancel,
        claimAndSaveChanges,
        saveChanges,
        saveText = viewModel.translate("actions.save"),
        saveClaimText = viewModel.translate("actions.save_claim"),
        cancelText = viewModel.translate("actions.cancel"),
    )

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

    val editPropertyData = remember(viewModel) { { sliderScrollToSectionItem(0, 2) } }
    val editLocation = remember(viewModel) { { sliderScrollToSectionItem(0, 3) } }
    val editFormData = remember(viewModel) { { index: Int -> sliderScrollToSection(index) } }
    InvalidSaveDialog(
        onEditLocation = editLocation,
        onEditPropertyData = editPropertyData,
        onEditFormData = editFormData,
    )
}

@Composable
private fun SectionPager(
    editSections: List<String>,
    modifier: Modifier = Modifier,
    snapToNearestIndex: () -> Unit = {},
    pagerState: LazyListState = rememberLazyListState(),
    scrollToSection: (Int) -> Unit = {},
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
                modifier = modifier
                    .clickable { scrollToSection(index) }
                    .listItemHeight(),
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

private fun LazyListScope.fullEditContent(
    caseData: CaseEditorUiState.CaseData,
    viewModel: CaseEditorViewModel,
    modifier: Modifier = Modifier,
    sectionTitles: List<String> = emptyList(),
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
    isPropertySectionCollapsed: Boolean = false,
    togglePropertySection: () -> Unit = {},
    isSectionCollapsed: (Int) -> Boolean = { false },
    toggleSection: (Int) -> Unit = {},
    translate: (String) -> String = { s -> s },
) {
    item(key = "incident-info") {
        val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
        CaseIncident(
            modifier,
            caseData.incident,
            caseData.isPendingSync,
            isSyncing = isSyncing,
        )
    }

    if (sectionTitles.isNotEmpty()) {
        viewModel.propertyEditor?.let { propertyEditor ->
            propertyLocationSection(
                viewModel,
                propertyEditor,
                sectionTitles[0],
                onMoveLocation,
                onSearchAddress,
                isSectionCollapsed = isPropertySectionCollapsed,
                togglePropertySection = togglePropertySection,
                translate = translate,
            )
        }

        viewModel.formDataEditors.forEachIndexed { index, editor ->
            item(
                key = "section-separator-$index",
                contentType = SectionSeparatorContentType,
            ) {
                SectionSeparator()
            }

            val sectionIndex = index + 1
            val sectionTitle =
                if (sectionIndex < sectionTitles.size) sectionTitles[sectionIndex] else ""
            formDataSection(
                viewModel,
                editor.inputData,
                sectionTitle,
                sectionIndex,
                isSectionCollapsed(sectionIndex),
                toggleSection,
            )
        }
    }
}

@Composable
private fun SectionSeparator() {
    Box(
        Modifier
            .fillMaxWidth()
            // TODO Common dimensions
            .height(32.dp)
            .background(color = separatorColor)
    )
}

private fun LazyListScope.propertyLocationSection(
    viewModel: CaseEditorViewModel,
    propertyEditor: CasePropertyDataEditor,
    sectionTitle: String,
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
    isSectionCollapsed: Boolean = false,
    togglePropertySection: () -> Unit = {},
    translate: (String) -> String = { s -> s },
) {
    item(
        key = "section-header-0",
        contentType = SectionHeaderContentType,
    ) {
        SectionHeaderCollapsible(
            viewModel,
            sectionIndex = 0,
            sectionTitle = sectionTitle,
            isCollapsed = isSectionCollapsed,
            toggleCollapse = togglePropertySection,
        )
    }
    if (!isSectionCollapsed) {
        item(key = "section-property") {
            PropertyFormView(
                viewModel,
                propertyEditor,
                translate = translate,
            )
        }

        viewModel.locationEditor?.let { locationEditor ->
            item(key = "section-location") {
                PropertyLocationView(
                    viewModel,
                    locationEditor,
                    onMoveLocationOnMap = onMoveLocation,
                    openAddressSearch = onSearchAddress,
                    translate = translate,
                )
            }
        }

        viewModel.notesFlagsEditor?.let { notesFlagsEditor ->
            item(key = "section-notes-flags") {
                PropertyNotesFlagsView(
                    viewModel,
                    notesFlagsEditor,
                    viewModel.visibleNoteCount,
                    translate = translate,
                )
            }
        }
    }
}

private fun LazyListScope.formDataSection(
    viewModel: CaseEditorViewModel,
    inputData: FormFieldsInputData,
    sectionTitle: String,
    sectionIndex: Int,
    isSectionCollapsed: Boolean = false,
    toggleSectionCollapse: (Int) -> Unit = {},
) {
    item(
        key = "section-header-$sectionIndex",
        contentType = SectionHeaderContentType,
    ) {
        val toggle = remember(viewModel) { { toggleSectionCollapse(sectionIndex) } }
        SectionHeaderCollapsible(
            viewModel,
            sectionIndex = sectionIndex,
            sectionTitle = sectionTitle,
            isCollapsed = isSectionCollapsed,
            toggleCollapse = toggle,
            help = inputData.helpText,
        )
    }

    if (!isSectionCollapsed) {
        item(
            key = "section-$sectionIndex",
        ) {
            FormDataItems(viewModel, inputData, LocalCaseEditor.current.isEditable)
        }
    }
}

@Composable
internal fun CaseIncident(
    modifier: Modifier = Modifier,
    incident: Incident = EmptyIncident,
    isPendingSync: Boolean = false,
    isSyncing: Boolean = false,
) {
    val incidentName = incident.shortName
    val disasterResId = getDisasterIcon(incident.disaster)
    Row(
        modifier = modifier.listItemPadding(),
        verticalAlignment = Alignment.CenterVertically,
        // TODO Common dimensions
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = incidentDisasterContainerColor,
            contentColor = incidentDisasterContentColor,
        ) {
            Icon(
                painter = painterResource(disasterResId),
                contentDescription = incidentName,
            )
        }
        Text(
            incidentName,
            Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
        )

        if (isSyncing) {
            Icon(
                imageVector = CrisisCleanupIcons.CloudSync,
                contentDescription = stringResource(R.string.is_syncing),
            )
        } else if (isPendingSync) {
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
    onEditFormData: (Int) -> Unit = {},
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {
    val promptInvalidSave by viewModel.showInvalidWorksiteSave.collectAsStateWithLifecycle()
    if (promptInvalidSave) {
        val invalidWorksiteInfo = viewModel.invalidWorksiteInfo.value
        if (invalidWorksiteInfo.invalidSection != WorksiteSection.None) {
            val message = invalidWorksiteInfo.message.ifBlank {
                val messageResId =
                    if (invalidWorksiteInfo.messageResId == 0) R.string.incomplete_required_data
                    else invalidWorksiteInfo.messageResId
                stringResource(messageResId)
            }
            val onDismiss =
                remember(viewModel) { { viewModel.showInvalidWorksiteSave.value = false } }
            AlertDialog(
                title = { Text(stringResource(R.string.incomplete_worksite)) },
                text = { Text(message) },
                onDismissRequest = onDismiss,
                dismissButton = {
                    CrisisCleanupTextButton(
                        text = viewModel.translate("actions.cancel"),
                        onClick = onDismiss
                    )
                },
                confirmButton = {
                    CrisisCleanupTextButton(
                        textResId = R.string.fix,
                        onClick = {
                            when (val section = invalidWorksiteInfo.invalidSection) {
                                WorksiteSection.Location -> onEditLocation()
                                WorksiteSection.Property -> onEditPropertyData()
                                WorksiteSection.Details -> onEditFormData(1)
                                WorksiteSection.WorkType -> onEditFormData(2)
                                WorksiteSection.Hazards -> onEditFormData(3)
                                WorksiteSection.VolunteerReport -> onEditFormData(4)
                                else -> {
                                    Log.w(
                                        "case-edit-invalid-alert",
                                        "Section $section is not configured for invalid alert",
                                    )
                                }
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
    onCancel: () -> Unit = {},
    onClaimAndSave: () -> Unit = {},
    onSave: () -> Unit = {},
    saveText: String = "",
    saveClaimText: String = "",
    cancelText: String = "",
) {
    val dimensions = LocalDimensions.current
    val isSharpCorners = dimensions.isThinScreenWidth
    Row(
        modifier = Modifier
            .padding(
                horizontal = dimensions.edgePaddingFlexible,
                vertical = dimensions.edgePadding,
            ),
        horizontalArrangement = dimensions.itemInnerSpacingHorizontalFlexible,
    ) {
        BusyButton(
            Modifier.weight(1f),
            text = cancelText,
            enabled = enable,
            onClick = onCancel,
            colors = cancelButtonColors(),
            isSharpCorners = isSharpCorners,
        )
        BusyButton(
            Modifier.weight(1.5f),
            text = saveClaimText,
            enabled = enable,
            indicateBusy = !enable,
            onClick = onClaimAndSave,
            isSharpCorners = isSharpCorners,
        )
        BusyButton(
            Modifier.weight(1.1f),
            text = saveText,
            enabled = enable,
            indicateBusy = !enable,
            onClick = onSave,
            isSharpCorners = isSharpCorners,
        )
    }
}

@Preview
@Composable
private fun CaseIncidentPreview() {
    CrisisCleanupTheme {
        Surface {
            CaseIncident(
                incident = EmptyIncident.copy(
                    name = "Big sweeping hurricane across the gulf",
                    shortName = "Big hurricane"
                ),
                isPendingSync = true,
            )
        }
    }
}

@Preview
@Composable
private fun SaveActionBarPreview() {
    CrisisCleanupTheme {
        SaveActionBar(
            saveText = "save",
            saveClaimText = "+ claim",
            cancelText = "cancel",
        )
    }
}
