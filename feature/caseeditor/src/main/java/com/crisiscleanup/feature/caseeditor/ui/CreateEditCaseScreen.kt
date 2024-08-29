package com.crisiscleanup.feature.caseeditor.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appnav.ViewImageArgs
import com.crisiscleanup.core.appnav.WorksiteImagesArgs
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.FocusSectionSlider
import com.crisiscleanup.core.designsystem.component.FormListSectionSeparator
import com.crisiscleanup.core.designsystem.component.LIST_DETAIL_DETAIL_WEIGHT
import com.crisiscleanup.core.designsystem.component.LIST_DETAIL_LIST_WEIGHT
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.component.listDetailDetailMaxWidth
import com.crisiscleanup.core.designsystem.component.rememberFocusSectionSliderState
import com.crisiscleanup.core.designsystem.component.rememberSectionContentIndexLookup
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.ImageCategory
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseEditorViewState
import com.crisiscleanup.feature.caseeditor.CasePropertyDataEditor
import com.crisiscleanup.feature.caseeditor.CreateEditCaseViewModel
import com.crisiscleanup.feature.caseeditor.WorksiteSection
import com.crisiscleanup.feature.caseeditor.model.FormFieldsInputData
import com.crisiscleanup.core.common.R as commonR

private const val SECTION_HEADER_CONTENT_TYPE = "section-header-content-type"
private const val SECTION_HEADER_SEPARATOR_TYPE = "section-header-separator-type"

@Composable
internal fun CreateEditCaseRoute(
    changeNewIncidentCase: (Long) -> Unit = {},
    changeExistingIncidentCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onOpenExistingCase: (ExistingWorksiteIdentifier) -> Unit = {},
    onEditSearchAddress: () -> Unit = {},
    onEditMoveLocationOnMap: () -> Unit = {},
    onBack: () -> Unit = {},
    openPhoto: (WorksiteImagesArgs) -> Unit = { _ -> },
    viewModel: CreateEditCaseViewModel = hiltViewModel(),
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

            CompositionLocalProvider(LocalAppTranslator provides viewModel) {
                ArrangeLayout(
                    onEditSearchAddress = onEditSearchAddress,
                    onEditMoveLocationOnMap = onEditMoveLocationOnMap,
                    onBack = onBack,
                    openPhoto = openPhoto,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArrangeLayout(
    onEditSearchAddress: () -> Unit,
    onEditMoveLocationOnMap: () -> Unit,
    onBack: () -> Unit,
    openPhoto: (WorksiteImagesArgs) -> Unit = { _ -> },
    viewModel: CreateEditCaseViewModel = hiltViewModel(),
) {
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

    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val areEditorsReady by viewModel.areEditorsReady.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSavingWorksite.collectAsStateWithLifecycle()
    var isCaseLoaded = false
    var isEditable = false
    (viewState as? CaseEditorViewState.CaseData)?.let { caseData ->
        isCaseLoaded = true
        isEditable = areEditorsReady && caseData.isNetworkLoadFinished && !isSaving
    }

    val isListDetailLayout = LocalDimensions.current.isListDetailWidth
    val screenModifier = Modifier.background(color = Color.White)
    if (isListDetailLayout) {
        Row(screenModifier) {
            Column(Modifier.weight(LIST_DETAIL_LIST_WEIGHT)) {
                TopAppBarBackAction(
                    title = headerTitle,
                    onAction = onNavigateBack,
                )

                Spacer(Modifier.weight(1f))

                if (isCaseLoaded) {
                    KeyboardSaveActionBar(
                        enable = isEditable,
                        isSaving = isSaving,
                        onCancel = onNavigateCancel,
                    )
                }
            }
            Column(
                Modifier
                    .weight(LIST_DETAIL_DETAIL_WEIGHT)
                    .sizeIn(maxWidth = listDetailDetailMaxWidth),
            ) {
                CreateEditCaseContent(
                    viewState,
                    isEditable = isEditable,
                    onEditSearchAddress = onEditSearchAddress,
                    onEditMoveLocationOnMap = onEditMoveLocationOnMap,
                    openPhoto = openPhoto,
                )
            }
        }
    } else {
        Column(screenModifier) {
            TopAppBarBackAction(
                title = headerTitle,
                onAction = onNavigateBack,
            )

            CreateEditCaseContent(
                viewState,
                isEditable = isEditable,
                onEditSearchAddress = onEditSearchAddress,
                onEditMoveLocationOnMap = onEditMoveLocationOnMap,
                openPhoto = openPhoto,
            ) {
                KeyboardSaveActionBar(
                    enable = isEditable,
                    isSaving = isSaving,
                    onCancel = onNavigateCancel,
                    horizontalLayout = true,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CreateEditCaseContent(
    viewState: CaseEditorViewState,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEditSearchAddress: () -> Unit = {},
    onEditMoveLocationOnMap: () -> Unit = {},
    openPhoto: (WorksiteImagesArgs) -> Unit = { _ -> },
    bottomCaseContent: @Composable () -> Unit = {},
) {
    when (viewState) {
        is CaseEditorViewState.Loading -> {
            Box(
                modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                BusyIndicatorFloatingTopCenter(true)
            }
        }

        is CaseEditorViewState.CaseData -> {
            FullEditView(
                viewState,
                isEditable = isEditable,
                onSearchAddress = onEditSearchAddress,
                onMoveLocation = onEditMoveLocationOnMap,
                openPhoto = openPhoto,
            )

            bottomCaseContent()
        }

        else -> {
            val errorData = viewState as CaseEditorViewState.Error
            val errorMessage = if (errorData.errorResId != 0) {
                stringResource(errorData.errorResId)
            } else {
                errorData.errorMessage.ifBlank { stringResource(commonR.string.unexpected_error) }
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
    caseData: CaseEditorViewState.CaseData,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    viewModel: CreateEditCaseViewModel = hiltViewModel(),
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
    openPhoto: (WorksiteImagesArgs) -> Unit = { _ -> },
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
            5 to 19,
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

    val isSectionCollapsed =
        remember(viewModel) { { sectionIndex: Int -> sectionCollapseStates[sectionIndex] } }
    val toggleSectionCollapse = remember(viewModel) {
        { sectionIndex: Int ->
            sectionCollapseStates[sectionIndex] = !sectionCollapseStates[sectionIndex]
        }
    }
    val togglePropertySection = remember(viewModel) { { toggleSectionCollapse(0) } }
    val togglePhotosSection = remember(viewModel) {
        {
            toggleSectionCollapse(sectionCollapseStates.size - 1)
        }
    }

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
                        onOpenPhoto = openPhoto,
                        isPropertySectionCollapsed = sectionCollapseStates[0],
                        togglePropertySection = togglePropertySection,
                        isSectionCollapsed = isSectionCollapsed,
                        toggleSection = toggleSectionCollapse,
                        isPhotosCollapsed = sectionCollapseStates.last(),
                        togglePhotosSection = togglePhotosSection,
                    )
                }
            }
        }

        val isLoadingWorksite by viewModel.isLoading.collectAsStateWithLifecycle()
        BusyIndicatorFloatingTopCenter(isLoadingWorksite)
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

private fun LazyListScope.sectionSeparator(key: String) {
    item(
        key,
        contentType = SECTION_HEADER_SEPARATOR_TYPE,
    ) {
        FormListSectionSeparator()
    }
}

private fun LazyListScope.fullEditContent(
    caseData: CaseEditorViewState.CaseData,
    viewModel: CreateEditCaseViewModel,
    modifier: Modifier = Modifier,
    sectionTitles: List<String> = emptyList(),
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
    onOpenPhoto: (WorksiteImagesArgs) -> Unit = { _ -> },
    isPropertySectionCollapsed: Boolean = false,
    togglePropertySection: () -> Unit = {},
    isSectionCollapsed: (Int) -> Boolean = { false },
    toggleSection: (Int) -> Unit = {},
    isPhotosCollapsed: Boolean = false,
    togglePhotosSection: () -> Unit = {},
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
            sectionSeparator("section-separator-$index")

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

        photosSection(
            viewModel,
            sectionTitles.last(),
            isSectionCollapsed = isPhotosCollapsed,
            togglePhotosSection = togglePhotosSection,
            onOpenPhoto = onOpenPhoto,
        )
    }
}

private fun LazyListScope.propertyLocationSection(
    viewModel: CreateEditCaseViewModel,
    propertyEditor: CasePropertyDataEditor,
    sectionTitle: String,
    onMoveLocation: () -> Unit = {},
    onSearchAddress: () -> Unit = {},
    isSectionCollapsed: Boolean = false,
    togglePropertySection: () -> Unit = {},
) {
    item(
        key = "section-header-0",
        contentType = SECTION_HEADER_CONTENT_TYPE,
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
    viewModel: CreateEditCaseViewModel,
    inputData: FormFieldsInputData,
    sectionTitle: String,
    sectionIndex: Int,
    isSectionCollapsed: Boolean = false,
    toggleSectionCollapse: (Int) -> Unit = {},
) {
    item(
        key = "section-header-$sectionIndex",
        contentType = SECTION_HEADER_CONTENT_TYPE,
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

private fun LazyListScope.photosSection(
    viewModel: CreateEditCaseViewModel,
    sectionTitle: String,
    isSectionCollapsed: Boolean = false,
    togglePhotosSection: () -> Unit = {},
    onOpenPhoto: (WorksiteImagesArgs) -> Unit = { _ -> },
) {
    sectionSeparator("section-separator-photos")

    item(
        key = "section-header-photos",
        contentType = SECTION_HEADER_CONTENT_TYPE,
    ) {
        val t = LocalAppTranslator.current
        SectionHeaderCollapsible(
            viewModel,
            sectionIndex = 5,
            sectionTitle = sectionTitle,
            isCollapsed = isSectionCollapsed,
            toggleCollapse = togglePhotosSection,
        )
    }
    if (!isSectionCollapsed) {
        item(key = "section-photos") {
            var enablePagerScroll by remember { mutableStateOf(true) }
            val setEnablePagerScroll = remember(viewModel) {
                { b: Boolean -> enablePagerScroll = b }
            }

            val photos by viewModel.beforeAfterPhotos.collectAsStateWithLifecycle()
            val deletingImageIds by viewModel.deletingImageIds.collectAsStateWithLifecycle()
            val syncingWorksiteImage by viewModel.syncingWorksiteImage.collectAsStateWithLifecycle()

            val onUpdateImageCategory = remember(viewModel) {
                { imageCategory: ImageCategory ->
                    viewModel.addImageCategory = imageCategory
                }
            }

            val viewHeaderTitle by viewModel.headerTitle.collectAsStateWithLifecycle()
            val openWorksitePhotos = remember(viewModel, onOpenPhoto) {
                { imageArgs: ViewImageArgs ->
                    val worksiteId = viewModel.photosWorksiteId
                    onOpenPhoto(imageArgs.toWorksiteImageArgs(worksiteId))
                }
            }
            CasePhotoImageView(
                viewModel,
                setEnablePagerScroll,
                photos,
                syncingWorksiteImage,
                deletingImageIds,
                onUpdateImageCategory,
                viewHeaderTitle,
                360.dp,
                openWorksitePhotos,
            )
        }
    }
}

@Composable
private fun PromptChangesDialog(
    onStay: () -> Unit = {},
    onAbort: () -> Unit = {},
) {
    val t = LocalAppTranslator.current
    CrisisCleanupAlertDialog(
        title = t("caseForm.unsaved_changes"),
        text = t("caseForm.continue_edit_or_lose_changes"),
        onDismissRequest = onStay,
        dismissButton = {
            CrisisCleanupTextButton(
                text = t("caseForm.no"),
                onClick = onAbort,
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                text = t("caseForm.yes"),
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
    viewModel: CreateEditCaseViewModel = hiltViewModel(),
) {
    val promptInvalidSave by viewModel.showInvalidWorksiteSave.collectAsStateWithLifecycle()
    if (promptInvalidSave) {
        val invalidInfo = viewModel.invalidWorksiteInfo.value
        if (invalidInfo.invalidSection != WorksiteSection.None) {
            val t = LocalAppTranslator.current
            val message = invalidInfo.message.ifBlank {
                t("caseForm.missing_required_fields")
            }
            val onDismiss =
                remember(viewModel) { { viewModel.showInvalidWorksiteSave.value = false } }
            CrisisCleanupAlertDialog(
                title = t("caseForm.missing_required_fields_title"),
                text = message,
                onDismissRequest = onDismiss,
                dismissButton = {
                    CrisisCleanupTextButton(
                        text = t("actions.cancel"),
                        onClick = onDismiss,
                    )
                },
                confirmButton = {
                    CrisisCleanupTextButton(
                        text = t("actions.fix"),
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
private fun KeyboardSaveActionBar(
    enable: Boolean,
    isSaving: Boolean,
    onCancel: () -> Unit,
    horizontalLayout: Boolean = false,
    viewModel: CreateEditCaseViewModel = hiltViewModel(),
) {
    val isKeyboardOpen = rememberIsKeyboardOpen()
    if (!isKeyboardOpen) {
        val claimAndSaveChanges = remember(viewModel) { { viewModel.saveChanges(true) } }
        val saveChanges = remember(viewModel) { { viewModel.saveChanges(false) } }
        val t = LocalAppTranslator.current
        SaveActionBar(
            enable = enable,
            isSaving = isSaving,
            onCancel = onCancel,
            onClaimAndSave = claimAndSaveChanges,
            onSave = saveChanges,
            saveText = t("actions.save"),
            saveClaimText = t("actions.save_claim"),
            cancelText = t("actions.cancel"),
            horizontalLayout = horizontalLayout,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    horizontalLayout: Boolean = false,
) {
    val dimensions = LocalDimensions.current
    val isSharpCorners = dimensions.isThinScreenWidth
    val rowMaxItemCount = if (horizontalLayout) Int.MAX_VALUE else 1
    FlowRow(
        modifier = Modifier.padding(
            horizontal = dimensions.edgePaddingFlexible,
            vertical = dimensions.edgePadding,
        ),
        horizontalArrangement = dimensions.itemInnerSpacingHorizontalFlexible,
        verticalArrangement = listItemSpacedBy,
        maxItemsInEachRow = rowMaxItemCount,
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
                .testTag("caseEditSaveBtn")
                .weight(1.1f),
            text = saveText,
            enabled = enable,
            indicateBusy = isSaving,
            onClick = onSave,
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
