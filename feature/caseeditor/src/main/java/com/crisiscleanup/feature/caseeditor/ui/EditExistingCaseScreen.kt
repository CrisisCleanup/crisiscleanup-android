package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appnav.ViewImageArgs
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.common.urlEncode
import com.crisiscleanup.core.designsystem.AppTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupFab
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.component.actionEdgeSpace
import com.crisiscleanup.core.designsystem.component.fabPlusSpaceHeight
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.designsystem.theme.primaryRedColor
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.core.ui.LinkifyEmailText
import com.crisiscleanup.core.ui.LinkifyLocationText
import com.crisiscleanup.core.ui.LinkifyPhoneText
import com.crisiscleanup.feature.caseeditor.ExistingCaseViewModel
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.WorkTypeProfile
import com.crisiscleanup.feature.caseeditor.model.CaseImage
import com.crisiscleanup.feature.caseeditor.model.ImageCategory
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

// TODO Use/move common dimensions
internal val edgeSpacing = 16.dp
internal val edgeSpacingHalf = edgeSpacing.times(0.5f)

@Composable
internal fun EditExistingCaseRoute(
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
    openTransferWorkType: () -> Unit = {},
    openPhoto: (ViewImageArgs) -> Unit = { _ -> },
) {
    val isPendingTransfer by viewModel.transferWorkTypeProvider.isPendingTransfer
    if (isPendingTransfer) {
        openTransferWorkType()
    }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isBusy = isLoading || isSaving
    val isEditable = !(isBusy || isPendingTransfer)

    val toggleFavorite = remember(viewModel) { { viewModel.toggleFavorite() } }
    val toggleHighPriority = remember(viewModel) { { viewModel.toggleHighPriority() } }

    var clipboardContents by remember { mutableStateOf("") }
    val copyToClipboard = remember(viewModel) {
        { copyValue: String? ->
            if (copyValue?.isNotBlank() == true) {
                clipboardContents = copyValue
            }
        }
    }
    val onCaseLongPress =
        remember(viewModel) { { copyToClipboard(viewModel.editableWorksite.value.caseNumber) } }

    Box(Modifier.fillMaxSize()) {
        Column {
            val title by viewModel.headerTitle.collectAsStateWithLifecycle()
            val subTitle by viewModel.subTitle.collectAsStateWithLifecycle()
            val worksite by viewModel.editableWorksite.collectAsStateWithLifecycle()
            val isEmptyWorksite = worksite == EmptyWorksite
            TopBar(
                title,
                subTitle,
                isFavorite = worksite.isLocalFavorite,
                isHighPriority = worksite.hasHighPriorityFlag,
                onBack,
                isEmptyWorksite,
                toggleFavorite,
                toggleHighPriority,
                isEditable,
                onCaseLongPress,
            )

            val tabTitles by viewModel.tabTitles.collectAsStateWithLifecycle()
            if (isEmptyWorksite) {
                if (viewModel.worksiteIdArg == EmptyWorksite.id) {
                    Text(
                        stringResource(R.string.no_worksite_selected),
                        Modifier.listItemPadding(),
                    )
                } else {
                    Box(Modifier.fillMaxSize()) {
                        BusyIndicatorFloatingTopCenter(true)
                    }
                }
            } else if (tabTitles.isNotEmpty()) {
                val statusOptions by viewModel.statusOptions.collectAsStateWithLifecycle()
                val caseEditor = CaseEditor(
                    isEditable,
                    statusOptions,
                    false,
                )
                val appTranslator = remember(viewModel) {
                    AppTranslator(translator = viewModel)
                }
                CompositionLocalProvider(
                    LocalCaseEditor provides caseEditor,
                    LocalAppTranslator provides appTranslator,
                ) {
                    ExistingCaseContent(
                        tabTitles,
                        worksite,
                        isBusy,
                        openPhoto,
                        copyToClipboard,
                    )

                    BottomActions(
                        worksite,
                        onFullEdit,
                    )
                }
            }
        }

        CopiedToClipboard(clipboardContents)
    }
}

private fun getTopIconActionColor(
    isActive: Boolean,
    isEditable: Boolean,
): Color {
    var tint = if (isActive) primaryRedColor
    else neutralIconColor
    if (!isEditable) {
        tint = tint.disabledAlpha()
    }
    return tint
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
private fun TopBar(
    title: String,
    subTitle: String = "",
    isFavorite: Boolean = false,
    isHighPriority: Boolean = false,
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    toggleFavorite: () -> Unit = {},
    toggleHighPriority: () -> Unit = {},
    isEditable: Boolean = false,
    onCaseLongPress: () -> Unit = {},
) {
    // TODO Style components as necessary

    val titleContent = @Composable {
        Column(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onCaseLongPress,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title)

            if (subTitle.isNotBlank()) {
                Text(
                    subTitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    val navigationContent = @Composable {
        TopBarBackAction(action = onBack)
    }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (isLoading) {
        @Composable {}
    } else {
        @Composable {
            val translator = LocalAppTranslator.current.translator
            val highPriorityTranslateKey =
                if (isHighPriority) "actions.unmark_high_priority"
                else "flag.flag_high_priority"
            val highPriorityTint = getTopIconActionColor(isHighPriority, isEditable)
            CrisisCleanupIconButton(
                iconResId = R.drawable.ic_important_filled,
                contentDescription = translator(highPriorityTranslateKey),
                onClick = toggleHighPriority,
                enabled = isEditable,
                tint = highPriorityTint,
            )

            val iconResId = if (isFavorite) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
            val favoriteDescription =
                if (isFavorite) translator("actions.remove_favorite", R.string.remove_favorite)
                else translator("actions.save_as_favorite", R.string.save_as_favorite)
            val favoriteTint = getTopIconActionColor(isFavorite, isEditable)
            CrisisCleanupIconButton(
                iconResId = iconResId,
                contentDescription = favoriteDescription,
                onClick = toggleFavorite,
                enabled = isEditable,
                tint = favoriteTint,
            )
        }
    }
    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
        actions = actionsContent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.ExistingCaseContent(
    tabTitles: List<String>,
    worksite: Worksite,
    isLoading: Boolean = false,
    openPhoto: (ViewImageArgs) -> Unit = { _ -> },
    copyToClipboard: (String?) -> Unit = {},
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { tabTitles.size }
    val selectedTabIndex = pagerState.currentPage
    val coroutine = rememberCoroutineScope()
    TabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = @Composable { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                // TODO Common dimensions
                height = 2.dp,
                color = primaryOrangeColor,
            )
        },
    ) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                text = { Text(title) },
                selected = selectedTabIndex == index,
                onClick = {
                    coroutine.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )
        }
    }

    var enablePagerScroll by remember { mutableStateOf(true) }
    val setEnablePagerScroll = remember(pagerState) { { b: Boolean -> enablePagerScroll = b } }

    Box(Modifier.weight(1f)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = enablePagerScroll,
        ) { pagerIndex ->
            when (pagerIndex) {
                0 -> EditExistingCaseInfoView(
                    worksite,
                    copyToClipboard = copyToClipboard,
                )

                1 -> EditExistingCasePhotosView(
                    setEnablePagerScroll = setEnablePagerScroll,
                    onPhotoSelect = openPhoto,
                )

                2 -> EditExistingCaseNotesView(worksite)
            }
        }
        BusyIndicatorFloatingTopCenter(isLoading)
    }
}

@Composable
private fun BottomActions(
    worksite: Worksite,
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
) {
    val translator = LocalAppTranslator.current.translator
    val isEditable = LocalCaseEditor.current.isEditable
    var contentColor = Color.Black
    if (!isEditable) {
        contentColor = contentColor.disabledAlpha()
    }
    NavigationBar(
        containerColor = cardContainerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
    ) {
        existingCaseActions.forEachIndexed { index, action ->
            var label = translator(action.translationKey)
            if (action.translationKey.isNotBlank()) {
                if (label == action.translationKey && action.textResId != 0) {
                    label = stringResource(action.textResId)
                }
            }
            if (label.isBlank() && action.textResId != 0) {
                label = stringResource(action.textResId)
            }

            NavigationBarItem(
                selected = false,
                onClick = {
                    when (index) {
                        0 -> {}
                        1 -> {}
                        2 -> {}
                        3 -> onFullEdit(
                            ExistingWorksiteIdentifier(
                                worksite.incidentId,
                                worksite.id,
                            )
                        )
                    }
                },
                icon = {
                    if (action.iconResId != 0) {
                        Icon(
                            painter = painterResource(action.iconResId),
                            contentDescription = label,
                        )
                    } else if (action.imageVector != null) {
                        Icon(
                            imageVector = action.imageVector,
                            contentDescription = label,
                        )
                    }
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = contentColor,
                    unselectedTextColor = contentColor,
                    indicatorColor = CrisisCleanupNavigationDefaults.navigationIndicatorColor(),
                ),
                enabled = isEditable,
            )
        }
    }
}

@Composable
private fun PropertyInfoRow(
    image: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    isPhone: Boolean = false,
    isEmail: Boolean = false,
    isLocation: Boolean = false,
    locationQuery: String = "",
) {
    Row(
        modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = listItemSpacedBy,
    ) {
        Icon(
            imageVector = image,
            contentDescription = text,
        )

        val style = MaterialTheme.typography.bodyLarge
        if (isPhone) {
            LinkifyPhoneText(text)
        } else if (isEmail) {
            LinkifyEmailText(text)
        } else if (isLocation) {
            LinkifyLocationText(text, locationQuery)
        } else {
            Text(text, style = style)
        }
    }
}

@Composable
internal fun EditExistingCaseInfoView(
    worksite: Worksite,
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    copyToClipboard: (String?) -> Unit = {},
) {
    val mapMarkerIcon by viewModel.mapMarkerIcon.collectAsStateWithLifecycle()
    val workTypeProfile by viewModel.workTypeProfile.collectAsStateWithLifecycle()

    val claimAll = remember(viewModel) { { viewModel.claimAll() } }
    val requestAll = remember(viewModel) { { viewModel.requestAll() } }
    val releaseAll = remember(viewModel) { { viewModel.releaseAll() } }
    val updateWorkType =
        remember(viewModel) { { updated: WorkType -> viewModel.updateWorkType(updated) } }
    val requestWorkType =
        remember(viewModel) { { workType: WorkType -> viewModel.requestWorkType(workType) } }
    val releaseWorkType =
        remember(viewModel) { { workType: WorkType -> viewModel.releaseWorkType(workType) } }

    LazyColumn {
        item(key = "incident-info") {
            val caseData by viewModel.caseData.collectAsStateWithLifecycle()
            caseData?.let { caseState ->
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                val scheduleSync = remember(viewModel) { { viewModel.scheduleSync() } }
                CaseIncidentView(
                    Modifier,
                    caseState.incident,
                    caseState.isPendingSync,
                    isSyncing = isSyncing,
                    scheduleSync = scheduleSync,
                )
            }
        }

        propertyInfoItems(worksite, mapMarkerIcon, copyToClipboard)
        workItems(
            workTypeProfile,
            claimAll = claimAll,
            requestAll = requestAll,
            releaseAll = releaseAll,
            updateWorkType = updateWorkType,
            requestWorkType = requestWorkType,
            releaseWorkType = releaseWorkType,
        )
        volunteerReportItems(worksite)

        item {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(edgeSpacing)
            )
        }
    }
}

private fun LazyListScope.itemInfoSectionHeader(
    sectionIndex: Int,
    titleTranslateKey: String,
    trailingContent: (@Composable () -> Unit)? = null,
) = item(
    "section-header-$sectionIndex",
    "content-header-$sectionIndex",
) {
    SectionHeader(
        Modifier.padding(top = if (sectionIndex > 0) edgeSpacing else 0.dp),
        sectionIndex,
        LocalAppTranslator.current.translator(titleTranslateKey),
        trailingContent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.propertyInfoItems(
    worksite: Worksite,
    mapMarkerIcon: BitmapDescriptor? = null,
    copyToClipboard: (String?) -> Unit = {},
) {
    itemInfoSectionHeader(0, "caseForm.property_information")

    item(key = "section-content-property") {
        CardSurface {
            Column(Modifier.padding(top = edgeSpacingHalf)) {
                PropertyInfoRow(
                    CrisisCleanupIcons.Person,
                    worksite.name,
                    Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { copyToClipboard(worksite.name) }
                        )
                        .fillMaxWidth()
                        .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                )
                val phoneNumbers = listOf(worksite.phone1, worksite.phone2).filterNotBlankTrim()
                    .joinToString("; ")
                PropertyInfoRow(
                    CrisisCleanupIcons.Phone,
                    phoneNumbers,
                    Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { copyToClipboard(phoneNumbers) }
                        )
                        .fillMaxWidth()
                        .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                    isPhone = true,
                )
                worksite.email?.let {
                    if (it.isNotBlank()) {
                        PropertyInfoRow(
                            CrisisCleanupIcons.Mail,
                            it,
                            Modifier
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { copyToClipboard(worksite.email) }
                                )
                                .fillMaxWidth()
                                .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                            isEmail = true,
                        )
                    }
                }
                // TODO Predetermine if address and coordinates are mismatching and change query if so
                //      Or can alert if mismatch
                val hasWrongLocation = worksite.hasWrongLocationFlag
                val fullAddress = listOf(
                    worksite.address,
                    worksite.city,
                    worksite.state,
                    worksite.postalCode,
                ).combineTrimText()
                val coordinates = worksite.coordinates()
                val locationQuery = if (hasWrongLocation) ""
                else "geo:${coordinates.latitude},${coordinates.longitude}?q=$fullAddress"
                PropertyInfoRow(
                    CrisisCleanupIcons.Location,
                    fullAddress,
                    Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { copyToClipboard(fullAddress) }
                        )
                        .fillMaxWidth()
                        .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                    isLocation = !worksite.hasWrongLocationFlag,
                    locationQuery = locationQuery,
                )

                PropertyInfoMapView(
                    worksite.coordinates(),
                    // TODO Common dimensions
                    Modifier
                        .height(192.dp)
                        .padding(top = edgeSpacingHalf),
                    mapMarkerIcon = mapMarkerIcon,
                )
            }
        }
    }
}

private fun LazyListScope.workItems(
    workTypeProfile: WorkTypeProfile? = null,
    claimAll: () -> Unit = {},
    releaseAll: () -> Unit = {},
    requestAll: () -> Unit = {},
    updateWorkType: (WorkType) -> Unit = {},
    requestWorkType: (WorkType) -> Unit = {},
    releaseWorkType: (WorkType) -> Unit = {},
) {
    workTypeProfile?.let { profile ->
        itemInfoSectionHeader(2, "caseForm.work") {
            Column(
                Modifier.padding(start = edgeSpacing),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(edgeSpacingHalf),
            ) {
                val translator = LocalAppTranslator.current.translator
                if (profile.unclaimed.isNotEmpty()) {
                    WorkTypePrimaryAction(translator("actions.claim_all_alt"), claimAll)
                }
                if (profile.releasableCount > 0) {
                    WorkTypeAction(translator("actions.release_all"), releaseAll)
                } else if (profile.requestableCount > 0) {
                    WorkTypeAction(translator("actions.request_all"), requestAll)
                }
            }
        }

        val rowItemModifier = Modifier.padding(horizontal = edgeSpacing)

        if (profile.otherOrgClaims.isNotEmpty()) {
            profile.otherOrgClaims.forEach { otherOrgClaim ->
                organizationWorkClaims(
                    otherOrgClaim,
                    rowItemModifier,
                    updateWorkType,
                    requestWorkType,
                    releaseWorkType,
                )
            }
        }

        if (profile.orgClaims.workTypes.isNotEmpty()) {
            organizationWorkClaims(
                profile.orgClaims,
                rowItemModifier,
                updateWorkType,
                requestWorkType,
                releaseWorkType,
            )
        }

        if (profile.unclaimed.isNotEmpty()) {
            workTypeSectionTitle("caseView.unclaimed_work_types", "unclaimed")
            existingWorkTypeItems(
                "unclaimed",
                profile.unclaimed,
                rowItemModifier,
                updateWorkType,
                requestWorkType,
                releaseWorkType,
            )
        }
    }
}

private fun LazyListScope.volunteerReportItems(
    worksite: Worksite,
) {
    // itemInfoSectionHeader(4, translator("caseView.report"))
}

@Composable
internal fun EditExistingCasePhotosView(
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    setEnablePagerScroll: (Boolean) -> Unit = {},
    onPhotoSelect: (ViewImageArgs) -> Unit = { _ -> },
) {
    val photos by viewModel.beforeAfterPhotos.collectAsStateWithLifecycle()
    val syncingWorksiteImage by viewModel.syncingWorksiteImage.collectAsStateWithLifecycle()

    var showCameraMediaSelect by remember { mutableStateOf(false) }

    val translator = LocalAppTranslator.current.translator
    val sectionTitleResIds = mapOf(
        ImageCategory.Before to translator("info.before_cleanup", R.string.before_cleanup),
        ImageCategory.After to translator("info.after_cleanup", R.string.after_cleanup),
    )
    // TODO Determine spacing and sizing based on available height.
    //      This viewport has
    //      - Top bar
    //      - Tab bar
    //      - Two rows of headers and items
    //      - Bottom bar
    //      - Optional snackbar which may wrap resulting in additional height
    val twoRowHeight = 256.dp
    val photoTwoRowModifier = Modifier
        .height(twoRowHeight)
        .listItemVerticalPadding()
    val photoOneRowModifier = Modifier
        .height(172.dp)
        .listItemVerticalPadding()
    val photoTwoRowGridCells = StaggeredGridCells.Adaptive(96.dp)
    val photoOneRowGridCells = StaggeredGridCells.Fixed(1)
    val isShortScreen = LocalConfiguration.current.screenHeightDp.dp < twoRowHeight.times(3)
    Column(
        modifier = Modifier.fillMaxHeight(),
    ) {
        sectionTitleResIds.onEach { (imageCategory, sectionTitle) ->
            photos[imageCategory]?.let { rowPhotos ->
                val isOneRow = isShortScreen || rowPhotos.size < 6
                PhotosSection(
                    sectionTitle,
                    if (isOneRow) photoOneRowModifier else photoTwoRowModifier,
                    if (isOneRow) photoOneRowGridCells else photoTwoRowGridCells,
                    photos = rowPhotos,
                    setEnableParentScroll = setEnablePagerScroll,
                    onAddPhoto = {
                        viewModel.addImageCategory = imageCategory
                        showCameraMediaSelect = true
                    },
                    onPhotoSelect = { image: CaseImage ->
                        with(image) {
                            val viewImageArgs = ViewImageArgs(
                                id,
                                encodedUri = if (isNetworkImage) imageUri.urlEncode() else "",
                                isNetworkImage,
                                viewModel.headerTitle.value.urlEncode(),
                            )
                            onPhotoSelect(viewImageArgs)
                        }
                    },
                    syncingWorksiteImage = syncingWorksiteImage,
                )
            }
        }
    }

    val closeCameraMediaSelect = remember(viewModel) { { showCameraMediaSelect = false } }
    TakePhotoSelectImage(
        showOptions = showCameraMediaSelect,
        closeOptions = closeCameraMediaSelect,
    )
}

@Composable
internal fun EditExistingCaseNotesView(
    worksite: Worksite,
    viewModel: ExistingCaseViewModel = hiltViewModel(),
) {
    val isEditable = LocalCaseEditor.current.isEditable

    var isCreatingNote by remember { mutableStateOf(false) }
    val onAddNote = remember(viewModel) {
        {
            if (isEditable) {
                isCreatingNote = true
            }
        }
    }

    val listState = rememberLazyListState()
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (newNoteFab) = createRefs()

        val notes = worksite.notes
        LazyColumn(
            state = listState,
            verticalArrangement = listItemSpacedBy,
        ) {
            item {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        // TODO Common dimensions
                        .height(8.dp)
                )
            }
            staticNoteItems(
                notes,
                notes.size,
                // TODO Common dimensions
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                true,
            )
            item {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .fabPlusSpaceHeight()
                )
            }
        }

        CrisisCleanupFab(
            onClick = onAddNote,
            modifier = Modifier.constrainAs(newNoteFab) {
                end.linkTo(parent.end, margin = actionEdgeSpace)
                bottom.linkTo(parent.bottom, margin = actionEdgeSpace)
            },
            enabled = isEditable,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_note),
                contentDescription = LocalAppTranslator.current.translator("caseView.add_note_alt"),
            )
        }

        if (viewModel.takeNoteAdded()) {
            LaunchedEffect(Unit) {
                listState.animateScrollToItem(0)
            }
        }
    }

    if (isCreatingNote) {
        val dismissNoteDialog = { isCreatingNote = false }
        val saveNote = remember(viewModel) {
            { note: WorksiteNote -> viewModel.saveNote(note) }
        }
        OnCreateNote(saveNote, dismissNoteDialog)
    }
}

data class IconTextAction(
    @DrawableRes val iconResId: Int = 0,
    val imageVector: ImageVector? = null,
    @StringRes val textResId: Int = 0,
    val translationKey: String = "",
)

// TODO Use translations where available
private val existingCaseActions = listOf(
    IconTextAction(
        iconResId = R.drawable.ic_share_small,
        translationKey = "actions.share",
    ),
    IconTextAction(
        iconResId = R.drawable.ic_flag_small,
        // TODO "actions.flag" is already taken and not "Flag"
        translationKey = "events.object_flag",
        textResId = R.string.flag,
    ),
    IconTextAction(
        iconResId = R.drawable.ic_history_small,
        translationKey = "actions.history",
    ),
    IconTextAction(
        iconResId = R.drawable.ic_edit_underscore_small,
        translationKey = "actions.edit",
    ),
)

@Composable
private fun PropertyInfoMapView(
    coordinates: LatLng,
    modifier: Modifier = Modifier,
    mapMarkerIcon: BitmapDescriptor? = null,
    onMapLoaded: () -> Unit = {},
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
) {
    val uiSettings by rememberMapUiSettings(
        zoomControls = true,
        disablePanning = true,
    )

    val markerState = rememberMarkerState()
    markerState.position = coordinates

    val update = CameraUpdateFactory.newLatLngZoom(coordinates, 13f)
    cameraPositionState.move(update)

    val mapProperties by rememberMapProperties()
    GoogleMap(
        modifier = modifier,
        uiSettings = uiSettings,
        properties = mapProperties,
        cameraPositionState = cameraPositionState,
        onMapLoaded = onMapLoaded,
    ) {
        Marker(
            markerState,
            icon = mapMarkerIcon,
        )
    }
}
