package com.crisiscleanup.feature.caseeditor.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.FocusSectionSlider
import com.crisiscleanup.core.designsystem.component.FormListSectionSeparator
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.component.rememberFocusSectionSliderState
import com.crisiscleanup.core.designsystem.component.rememberSectionContentIndexLookup
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseEditorUiState
import com.crisiscleanup.feature.caseeditor.CaseEditorViewModel
import com.crisiscleanup.feature.caseeditor.CasePropertyDataEditor
import com.crisiscleanup.feature.caseeditor.WorksiteSection
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import com.crisiscleanup.core.common.R as commonR

private const val SectionHeaderContentType = "section-header-content-type"
private const val SectionSeparatorContentType = "section-header-content-type"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseEditorRoute(
    changeNewIncidentCase: (Long) -> Unit = {},
    changeExistingIncidentCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onOpenExistingCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onEditSearchAddress: () -> Unit = {},
    onEditMoveLocationOnMap: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {
    val changeWorksiteIncidentId by viewModel.changeWorksiteIncidentId.collectAsStateWithLifecycle()
    val changeExistingWorksite by viewModel.changeExistingWorksite.collectAsStateWithLifecycle()
    val editDifferentWorksite by viewModel.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        onOpenExistingCase(editDifferentWorksite)
    } else if (changeWorksiteIncidentId != EmptyIncident.id) {
        changeNewIncidentCase(changeWorksiteIncidentId)
    } else if (changeExistingWorksite.isDefined) {
        changeExistingIncidentCase(changeExistingWorksite)
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

                CompositionLocalProvider(LocalAppTranslator provides viewModel) {
                    CaseEditorScreen(
                        onNavigateCancel = onNavigateCancel,
                        onEditSearchAddress = onEditSearchAddress,
                        onEditMoveLocationOnMap = onEditMoveLocationOnMap,
                    )
                }
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
                    .weight(1f),
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
            val errorMessage = if (errorData.errorResId != 0) {
                stringResource(errorData.errorResId)
            } else {
                errorData.errorMessage.ifEmpty { stringResource(commonR.string.unexpected_error) }
            }
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

    // Content list sections to list item index. Trace fullEditContent.
    val indexLookups by rememberSectionContentIndexLookup(
        mapOf(
            0 to 1,
            1 to 7,
            2 to 10,
            3 to 13,
            4 to 16,
        ),
    )

    val sectionSliderState = rememberFocusSectionSliderState(
        viewModel,
        sectionCollapseStates,
        indexLookups,
    )

    FocusSectionSlider(
        editSections,
        sectionSliderState,
        indexLookups,
        sectionCollapseStates,
    )

    val areEditorsReady by viewModel.areEditorsReady.collectAsStateWithLifecycle()
    val isSavingData by viewModel.isSavingWorksite.collectAsStateWithLifecycle()
    val isEditable = areEditorsReady && caseData.isNetworkLoadFinished && !isSavingData

    val isSectionCollapsed =
        remember(viewModel) { { sectionIndex: Int -> sectionCollapseStates[sectionIndex] } }
    val toggleSectionCollapse = remember(viewModel) {
        { sectionIndex: Int ->
            sectionCollapseStates[sectionIndex] = !sectionCollapseStates[sectionIndex]
        }
    }
    val togglePropertySection = remember(viewModel) { { toggleSectionCollapse(0) } }

    Box(Modifier.weight(1f)) {
        val closeKeyboard = rememberCloseKeyboard(viewModel)
        val onScrollFling = remember(viewModel) {
            {
                closeKeyboard()
                viewModel.clearFocusScrollToSection()
            }
        }

        val caseEditor = CaseEditor(
            isEditable,
            caseData.statusOptions,
            caseData.worksite.isNew,
        )
        CompositionLocalProvider(LocalCaseEditor provides caseEditor) {
            LazyColumn(
                modifier
                    .scrollFlingListener(onScrollFling)
                    .fillMaxSize(),
                state = sectionSliderState.contentListState,
            ) {
                if (editSections.isNotEmpty()) {
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
                    )
                }
            }
        }

        val isLoadingWorksite by viewModel.isLoading.collectAsStateWithLifecycle()
        BusyIndicatorFloatingTopCenter(isLoadingWorksite)
    }

    val isKeyboardOpen = rememberIsKeyboardOpen()
    if (!isKeyboardOpen) {
        val claimAndSaveChanges = remember(viewModel) { { viewModel.saveChanges(true) } }
        val saveChanges = remember(viewModel) { { viewModel.saveChanges(false) } }
        val translator = LocalAppTranslator.current
        SaveActionBar(
            enable = isEditable,
            isSaving = isSavingData,
            onCancel = onCancel,
            onClaimAndSave = claimAndSaveChanges,
            onSave = saveChanges,
            saveText = translator("actions.save"),
            saveClaimText = translator("actions.save_claim"),
            cancelText = translator("actions.cancel"),
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

    val sliderScrollToSectionItem = sectionSliderState.sliderScrollToSectionItem
    val sliderScrollToSection = sectionSliderState.sliderScrollToSection
    val editPropertyData = remember(viewModel) { { sliderScrollToSectionItem(0, 2, 0) } }
    val editLocation = remember(viewModel) { { sliderScrollToSectionItem(0, 3, 48) } }
    val editLocationAddress = remember(viewModel) { { sliderScrollToSectionItem(0, 4, 0) } }
    val editFormData = remember(viewModel) { { index: Int -> sliderScrollToSection(index) } }
    InvalidSaveDialog(
        onEditLocationAddress = editLocationAddress,
        onEditPropertyData = editPropertyData,
        onEditLocation = editLocation,
        onEditFormData = editFormData,
    )

    val focusScrollToSection by viewModel.focusScrollToSection.collectAsStateWithLifecycle()
    if (focusScrollToSection.first != 0 ||
        focusScrollToSection.second != 0 ||
        focusScrollToSection.third != 0
    ) {
        sliderScrollToSectionItem(
            focusScrollToSection.first,
            focusScrollToSection.second,
            focusScrollToSection.third,
        )
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
) {
    item(key = "incident-info") {
        val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
        val scheduleSync = remember(viewModel) { { viewModel.scheduleSync() } }
        CaseIncidentView(
            modifier,
            caseData.incident,
            caseData.isPendingSync,
            isSyncing = isSyncing,
            scheduleSync = scheduleSync,
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
            )
        }

        viewModel.formDataEditors.forEachIndexed { index, editor ->
            item(
                key = "section-separator-$index",
                contentType = SectionSeparatorContentType,
            ) {
                FormListSectionSeparator()
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

private fun LazyListScope.propertyLocationSection(
    viewModel: CaseEditorViewModel,
    propertyEditor: CasePropertyDataEditor,
    sectionTitle: String,
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
    isSectionCollapsed: Boolean = false,
    togglePropertySection: () -> Unit = {},
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
            )
        }

        viewModel.locationEditor?.let { locationEditor ->
            item(key = "section-location") {
                val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                PropertyLocationView(
                    viewModel,
                    locationEditor,
                    isOnline,
                    onMoveLocationOnMap = onMoveLocation,
                    openAddressSearch = onSearchAddress,
                )
            }

            item(key = "section-location-address") {
                LocationFormView(locationEditor)
            }
        }

        viewModel.notesFlagsEditor?.let { notesFlagsEditor ->
            item(key = "section-notes-flags") {
                PropertyNotesFlagsView(
                    viewModel,
                    notesFlagsEditor,
                    viewModel.visibleNoteCount,
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
            FormDataItems(
                viewModel,
                inputData,
                LocalCaseEditor.current.isEditable,
            )
        }
    }
}

@Composable
private fun PromptChangesDialog(
    onStay: () -> Unit = {},
    onAbort: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    CrisisCleanupAlertDialog(
        title = translator("caseForm.unsaved_changes"),
        text = translator("caseForm.continue_edit_or_lose_changes"),
        onDismissRequest = onStay,
        dismissButton = {
            CrisisCleanupTextButton(
                text = translator("caseForm.no"),
                onClick = onAbort,
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                text = translator("caseForm.yes"),
                onClick = onStay,
            )
        },
    )
}

@Composable
private fun InvalidSaveDialog(
    onEditPropertyData: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    onEditLocationAddress: () -> Unit = {},
    onEditFormData: (Int) -> Unit = {},
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {
    val promptInvalidSave by viewModel.showInvalidWorksiteSave.collectAsStateWithLifecycle()
    if (promptInvalidSave) {
        val invalidInfo = viewModel.invalidWorksiteInfo.value
        if (invalidInfo.invalidSection != WorksiteSection.None) {
            val translator = LocalAppTranslator.current
            val message = invalidInfo.message.ifBlank {
                translator("caseForm.missing_required_fields")
            }
            val onDismiss =
                remember(viewModel) { { viewModel.showInvalidWorksiteSave.value = false } }
            CrisisCleanupAlertDialog(
                title = translator("caseForm.missing_required_fields_title"),
                text = message,
                onDismissRequest = onDismiss,
                dismissButton = {
                    CrisisCleanupTextButton(
                        text = translator("actions.cancel"),
                        onClick = onDismiss,
                    )
                },
                confirmButton = {
                    CrisisCleanupTextButton(
                        text = translator("actions.fix"),
                        onClick = {
                            when (val section = invalidInfo.invalidSection) {
                                WorksiteSection.Property -> onEditPropertyData()
                                WorksiteSection.Location -> onEditLocation()
                                WorksiteSection.LocationAddress -> onEditLocationAddress()
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
    enable: Boolean = false,
    isSaving: Boolean = false,
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
        modifier = Modifier.padding(
            horizontal = dimensions.edgePaddingFlexible,
            vertical = dimensions.edgePadding,
        ),
        horizontalArrangement = dimensions.itemInnerSpacingHorizontalFlexible,
    ) {
        val style = LocalFontStyles.current.header5
        BusyButton(
            Modifier
                .testTag("caseEditCancelBtn")
                .weight(1f),
            text = cancelText,
            enabled = enable,
            onClick = onCancel,
            colors = cancelButtonColors(),
            isSharpCorners = isSharpCorners,
            style = style,
        )
        BusyButton(
            Modifier
                .testTag("caseEditClaimAndSaveBtn")
                .weight(1.5f),
            text = saveClaimText,
            enabled = enable,
            indicateBusy = isSaving,
            onClick = onClaimAndSave,
            isSharpCorners = isSharpCorners,
            style = style,
        )
        BusyButton(
            Modifier
                .testTag("caseEditSaveBtn")
                .weight(1.1f),
            text = saveText,
            enabled = enable,
            indicateBusy = isSaving,
            onClick = onSave,
            isSharpCorners = isSharpCorners,
            style = style,
        )
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
