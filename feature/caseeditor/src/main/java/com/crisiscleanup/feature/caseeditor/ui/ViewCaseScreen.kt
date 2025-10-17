package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appnav.ViewImageArgs
import com.crisiscleanup.core.appnav.WorksiteImagesArgs
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.commoncase.model.addressQuery
import com.crisiscleanup.core.commoncase.ui.ExplainWrongLocationDialog
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.LocalLayoutProvider
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.CollapsibleIcon
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupFab
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.LIST_DETAIL_DETAIL_WEIGHT
import com.crisiscleanup.core.designsystem.component.LIST_DETAIL_LIST_WEIGHT
import com.crisiscleanup.core.designsystem.component.LeadingIconChip
import com.crisiscleanup.core.designsystem.component.LinkifyEmailText
import com.crisiscleanup.core.designsystem.component.LinkifyLocationText
import com.crisiscleanup.core.designsystem.component.LinkifyPhoneText
import com.crisiscleanup.core.designsystem.component.MapViewToggleButton
import com.crisiscleanup.core.designsystem.component.TemporaryDialog
import com.crisiscleanup.core.designsystem.component.WorkTypeBusyAction
import com.crisiscleanup.core.designsystem.component.WorkTypePrimaryAction
import com.crisiscleanup.core.designsystem.component.actionEdgeSpace
import com.crisiscleanup.core.designsystem.component.fabPlusSpaceHeight
import com.crisiscleanup.core.designsystem.component.listDetailDetailMaxWidth
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.ImageCategory
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.ViewCaseViewModel
import com.crisiscleanup.feature.caseeditor.WorkTypeProfile
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch

// TODO Use/move common dimensions
internal val edgeSpacing = 16.dp
internal val edgeSpacingHalf = edgeSpacing.times(0.5f)

private val flagColorFallback = Color(0xFF000000)
private val flagColors = mapOf(
    WorksiteFlagType.HighPriority to Color(0xFF367bc3),
    WorksiteFlagType.UpsetClient to Color(0xFF00b3bf),
    WorksiteFlagType.ReportAbuse to Color(0xFFd79425),
    WorksiteFlagType.WrongLocation to Color(0xFFf77020),
    WorksiteFlagType.WrongIncident to Color(0xFFc457e7),
)

@Composable
internal fun EditExistingCaseRoute(
    viewModel: ViewCaseViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onBackToCases: () -> Unit = {},
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
    openTransferWorkType: () -> Unit = {},
    openPhoto: (WorksiteImagesArgs) -> Unit = { _ -> },
    openAddFlag: () -> Unit = {},
    openShareCase: () -> Unit = {},
    openCaseHistory: () -> Unit = {},
) {
    val isPendingTransfer by viewModel.transferWorkTypeProvider.isPendingTransfer
    if (isPendingTransfer) {
        openTransferWorkType()
    }

    val jumpToCaseOnMapOnBack by viewModel.jumpToCaseOnMapOnBack.collectAsStateWithLifecycle()
    if (jumpToCaseOnMapOnBack) {
        onBackToCases()
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

    val isRailNav = !LocalLayoutProvider.current.isBottomNav
    Box {
        val worksite by viewModel.editableWorksite.collectAsStateWithLifecycle()
        val openWorksiteImages = remember(worksite) {
            { args: ViewImageArgs ->
                openPhoto(args.toWorksiteImageArgs(worksite.id))
            }
        }
        Row {
            if (isRailNav) {
                ViewCaseNav(
                    worksite,
                    isEditable,
                    onFullEdit = onFullEdit,
                    onCaseFlags = openAddFlag,
                    onCaseShare = openShareCase,
                    onCaseHistory = openCaseHistory,
                )
            }

            val title by viewModel.headerTitle.collectAsStateWithLifecycle()
            val subTitle by viewModel.subTitle.collectAsStateWithLifecycle()
            val isEmptyWorksite = worksite == EmptyWorksite

            val tabTitles by viewModel.tabTitles.collectAsStateWithLifecycle()
            val updatedAtText by viewModel.updatedAtText.collectAsStateWithLifecycle()

            if (isRailNav) {
                ListDetailContent(
                    worksite = worksite,
                    title = title,
                    subTitle = subTitle,
                    onBack = onBack,
                    isEmptyWorksite = isEmptyWorksite,
                    toggleFavorite = toggleFavorite,
                    toggleHighPriority = toggleHighPriority,
                    isEditable = isEditable,
                    onCaseLongPress = onCaseLongPress,
                    updatedAtText = updatedAtText,
                    tabTitles = tabTitles,
                    isBusy = isBusy,
                    copyToClipboard = copyToClipboard,
                    openPhoto = openWorksiteImages,
                )
            } else {
                PortraitContent(
                    worksite = worksite,
                    title = title,
                    subTitle = subTitle,
                    onBack = onBack,
                    isEmptyWorksite = isEmptyWorksite,
                    toggleFavorite = toggleFavorite,
                    toggleHighPriority = toggleHighPriority,
                    isEditable = isEditable,
                    onCaseLongPress = onCaseLongPress,
                    updatedAtText = updatedAtText,
                    tabTitles = tabTitles,
                    isBusy = isBusy,
                    copyToClipboard = copyToClipboard,
                    onFullEdit = onFullEdit,
                    openPhoto = openWorksiteImages,
                    openAddFlag = openAddFlag,
                    openShareCase = openShareCase,
                    openCaseHistory = openCaseHistory,
                )
            }
        }

        CopiedToClipboard(clipboardContents)

        val actionDescription by viewModel.actionDescriptionMessage.collectAsStateWithLifecycle()
        TemporaryDialog(actionDescription)

        if (viewModel.isOverClaimingWork) {
            val closeDialog = remember(viewModel) { { viewModel.isOverClaimingWork = false } }
            OverClaimAlertDialog(closeDialog)
        }
    }
}

@Composable
private fun NonWorksiteView(showEmpty: Boolean) {
    if (showEmpty) {
        Text(
            LocalAppTranslator.current("info.no_worksite_selected"),
            Modifier.listItemPadding(),
        )
    } else {
        Box {
            BusyIndicatorFloatingTopCenter(true)
        }
    }
}

@Composable
fun ColumnScope.CaseContent(
    worksite: Worksite,
    isEditable: Boolean,
    tabTitles: List<String>,
    isBusy: Boolean,
    copyToClipboard: (String?) -> Unit,
    openPhoto: (ViewImageArgs) -> Unit,
    viewModel: ViewCaseViewModel = hiltViewModel(),
    nestedContent: @Composable ColumnScope.() -> Unit = {},
) {
    val statusOptions by viewModel.statusOptions.collectAsStateWithLifecycle()
    val caseEditor = CaseEditor(
        isEditable,
        statusOptions,
        false,
    )
    CompositionLocalProvider(
        LocalCaseEditor provides caseEditor,
        LocalAppTranslator provides viewModel,
    ) {
        ExistingCaseContent(
            tabTitles,
            worksite,
            isBusy,
            openPhoto,
            copyToClipboard,
        )
        nestedContent()
    }
}

@Composable
internal fun ViewCaseUpdatedAtView(
    updatedAtText: String,
    modifier: Modifier = Modifier,
) {
    if (updatedAtText.isNotBlank()) {
        Text(
            updatedAtText,
            modifier.testTag("editCaseUpdatedAtText"),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PortraitContent(
    worksite: Worksite,
    title: String,
    subTitle: String,
    onBack: () -> Unit,
    isEmptyWorksite: Boolean,
    toggleFavorite: () -> Unit,
    toggleHighPriority: () -> Unit,
    isEditable: Boolean,
    onCaseLongPress: () -> Unit,
    updatedAtText: String,
    tabTitles: List<String>,
    isBusy: Boolean,
    copyToClipboard: (String?) -> Unit,
    viewModel: ViewCaseViewModel = hiltViewModel(),
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
    openPhoto: (ViewImageArgs) -> Unit = { _ -> },
    openAddFlag: () -> Unit = {},
    openShareCase: () -> Unit = {},
    openCaseHistory: () -> Unit = {},
) {
    Column {
        ViewCaseHeader(
            title,
            subTitle,
            updatedAtText,
            isFavorite = worksite.isLocalFavorite,
            isHighPriority = worksite.hasHighPriorityFlag,
            onBack,
            isEmptyWorksite,
            toggleFavorite,
            toggleHighPriority,
            isEditable,
            onCaseLongPress,
            isTopBar = true,
        )

        ViewCaseUpdatedAtView(
            updatedAtText,
            modifier = Modifier
                .background(Color.White)
                .then(listItemModifier),
        )

        if (isEmptyWorksite) {
            NonWorksiteView(viewModel.worksiteIdArg == EmptyWorksite.id)
        } else if (tabTitles.isNotEmpty()) {
            CaseContent(
                worksite = worksite,
                isEditable = isEditable,
                tabTitles = tabTitles,
                isBusy = isBusy,
                copyToClipboard = copyToClipboard,
                openPhoto = openPhoto,
            ) {
                val isKeyboardOpen = rememberIsKeyboardOpen()
                if (!isKeyboardOpen) {
                    ViewCaseNav(
                        worksite,
                        isEditable,
                        onFullEdit = onFullEdit,
                        onCaseFlags = openAddFlag,
                        onCaseShare = openShareCase,
                        onCaseHistory = openCaseHistory,
                        isBottomNav = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListDetailContent(
    worksite: Worksite,
    title: String,
    subTitle: String,
    onBack: () -> Unit,
    isEmptyWorksite: Boolean,
    toggleFavorite: () -> Unit,
    toggleHighPriority: () -> Unit,
    isEditable: Boolean,
    onCaseLongPress: () -> Unit,
    updatedAtText: String,
    tabTitles: List<String>,
    isBusy: Boolean,
    copyToClipboard: (String?) -> Unit,
    viewModel: ViewCaseViewModel = hiltViewModel(),
    openPhoto: (ViewImageArgs) -> Unit = { _ -> },
) {
    Row {
        Column(Modifier.weight(LIST_DETAIL_LIST_WEIGHT)) {
            ViewCaseHeader(
                title,
                subTitle,
                updatedAtText,
                isFavorite = worksite.isLocalFavorite,
                isHighPriority = worksite.hasHighPriorityFlag,
                onBack,
                isEmptyWorksite,
                toggleFavorite,
                toggleHighPriority,
                isEditable,
                onCaseLongPress,
            )
        }
        Column(
            Modifier
                .weight(LIST_DETAIL_DETAIL_WEIGHT)
                .sizeIn(maxWidth = listDetailDetailMaxWidth),
        ) {
            if (isEmptyWorksite) {
                NonWorksiteView(viewModel.worksiteIdArg == EmptyWorksite.id)
            } else if (tabTitles.isNotEmpty()) {
                CaseContent(
                    worksite = worksite,
                    isEditable = isEditable,
                    tabTitles = tabTitles,
                    isBusy = isBusy,
                    copyToClipboard = copyToClipboard,
                    openPhoto = openPhoto,
                )
            }
        }
    }
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
    // TODO Page does not keep across first orientation change
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
    ) { tabTitles.size }
    val selectedTabIndex = pagerState.currentPage
    val coroutine = rememberCoroutineScope()
    TabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = @Composable { tabPositions ->
            SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                // TODO Common dimensions
                height = 2.dp,
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

    Box(Modifier.weight(1f)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = enablePagerScroll,
        ) { pagerIndex ->
            when (pagerIndex) {
                0 -> CaseInfoView(
                    worksite,
                    copyToClipboard = copyToClipboard,
                )

                1 -> CasePhotosView(
                    setEnablePagerScroll = setEnablePagerScroll,
                    onPhotoSelect = openPhoto,
                )

                2 -> CaseNotesView(worksite)
            }
        }
        BusyIndicatorFloatingTopCenter(isLoading && pagerState.currentPage > 0)
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
internal fun PropertyInfoRow(
    image: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    isPhone: Boolean = false,
    isEmail: Boolean = false,
    isLocation: Boolean = false,
    locationQuery: String = "",
    subText: String = "",
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedBy,
    ) {
        Icon(
            imageVector = image,
            contentDescription = text,
            tint = neutralIconColor,
        )

        Column(
            Modifier.weight(1f),
            verticalArrangement = listItemSpacedByHalf,
        ) {
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

            if (subText.isNotBlank()) {
                Text(
                    subText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        trailingContent?.invoke()
    }
}

@Composable
private fun CaseInfoView(
    worksite: Worksite,
    copyToClipboard: (String?) -> Unit = {},
    viewModel: ViewCaseViewModel = hiltViewModel(),
) {
    val mapMarkerIcon by viewModel.mapMarkerIcon.collectAsStateWithLifecycle()
    val isMapSatelliteView by viewModel.isMapSatelliteView.collectAsStateWithLifecycle(false)
    val workTypeProfile by viewModel.workTypeProfile.collectAsStateWithLifecycle()

    val removeFlag = remember(viewModel) { { flag: WorksiteFlag -> viewModel.removeFlag(flag) } }

    val claimAll = remember(viewModel) { { viewModel.claimAll() } }
    val requestAll = remember(viewModel) { { viewModel.requestAll() } }
    val releaseAll = remember(viewModel) { { viewModel.releaseAll() } }
    val updateWorkType = remember(viewModel) {
        { updated: WorkType, isStatusChange: Boolean ->
            viewModel.updateWorkType(updated, isStatusChange)
        }
    }
    val requestWorkType =
        remember(viewModel) { { workType: WorkType -> viewModel.requestWorkType(workType) } }
    val releaseWorkType =
        remember(viewModel) { { workType: WorkType -> viewModel.releaseWorkType(workType) } }

    val distanceAwayText by viewModel.distanceAwayText.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    LazyColumn(state = listState) {
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

                LaunchedEffect(Unit) {
                    listState.scrollToItem(0)
                }
            }
        }

        flagItems(worksite, removeFlag)
        propertyInfoItems(
            worksite,
            isMapSatelliteView,
            mapMarkerIcon,
            copyToClipboard,
            distanceAwayText,
            viewModel::setMapSatelliteView,
            viewModel::jumpToCaseOnMap,
        )
        workItems(
            workTypeProfile,
            claimAll = claimAll,
            requestAll = requestAll,
            releaseAll = releaseAll,
            updateWorkType = updateWorkType,
            requestWorkType = requestWorkType,
            releaseWorkType = releaseWorkType,
        )

        item {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(edgeSpacing),
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
        LocalAppTranslator.current(titleTranslateKey),
        trailingContent,
    )
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.flagItems(
    worksite: Worksite,
    removeFlag: (WorksiteFlag) -> Unit = {},
) {
    worksite.flags?.let { flags ->
        if (flags.isNotEmpty()) {
            item(key = "section-content-flags") {
                ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                    FlowRow(
                        listItemModifier,
                        horizontalArrangement = listItemSpacedBy,
                        verticalArrangement = listItemSpacedBy,
                    ) {
                        flags.forEach { flag -> FlagChip(flag, removeFlag) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagChip(
    flag: WorksiteFlag,
    removeFlag: (WorksiteFlag) -> Unit = {},
) {
    flag.flagType?.let { flagType ->
        val translator = LocalAppTranslator.current
        val isEditable = LocalCaseEditor.current.isEditable
        val color = flagColors[flagType] ?: flagColorFallback
        val text = translator(flagType.literal)
        val removeFlagTranslateKey = "actions.remove_type_flag"
        val description = translator(removeFlagTranslateKey).replace("{flag}", text)

        var contentColor = Color.White
        if (!isEditable) {
            contentColor = contentColor.disabledAlpha()
        }

        LeadingIconChip(
            text,
            { removeFlag(flag) },
            isEditable,
            containerColor = color,
            iconDescription = description,
            contentTint = contentColor,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.propertyInfoItems(
    worksite: Worksite,
    isMapSatelliteView: Boolean,
    mapMarkerIcon: BitmapDescriptor? = null,
    copyToClipboard: (String?) -> Unit = {},
    distanceAwayText: String = "",
    setMapSatelliteView: (Boolean) -> Unit = {},
    onJumpToCaseOnMap: () -> Unit = {},
) {
    itemInfoSectionHeader(0, "caseForm.property_information")

    item(key = "section-content-property") {
        CardSurface {
            Column(Modifier.padding(top = edgeSpacingHalf)) {
                PropertyInfoRow(
                    CrisisCleanupIcons.Person,
                    worksite.name,
                    Modifier
                        .testTag("editCasePropertyInfoWorksiteNameRow")
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { copyToClipboard(worksite.name) },
                        )
                        .fillMaxWidth()
                        .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                )
                val phoneNumbers =
                    listOf(worksite.phone1, worksite.phone2).filterNotBlankTrim().joinToString("; ")
                val phoneNotes =
                    listOf(worksite.phone1Notes, worksite.phone2Notes).filterNotBlankTrim()
                        .joinToString("\n")
                PropertyInfoRow(
                    CrisisCleanupIcons.Phone,
                    phoneNumbers,
                    Modifier
                        .testTag("editCasePropertyInfoPhoneRow")
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { copyToClipboard(phoneNumbers) },
                        )
                        .fillMaxWidth()
                        .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                    isPhone = true,
                    subText = phoneNotes,
                )
                worksite.email?.let {
                    if (it.isNotBlank()) {
                        PropertyInfoRow(
                            CrisisCleanupIcons.Mail,
                            it,
                            Modifier
                                .testTag("editCasePropertyInfoEmailRow")
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { copyToClipboard(worksite.email) },
                                )
                                .fillMaxWidth()
                                .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                            isEmail = true,
                        )
                    }
                }
                // TODO Show alert if wrong address is checked. Providing additional context.
                val (fullAddress, geoQuery, locationQuery) = worksite.addressQuery
                PropertyInfoRow(
                    CrisisCleanupIcons.Location,
                    fullAddress,
                    Modifier
                        .testTag("editCasePropertyInfoLocationRow")
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { copyToClipboard(fullAddress) },
                        )
                        .fillMaxWidth()
                        .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                    isLocation = true,
                    locationQuery = geoQuery.ifBlank { locationQuery },
                ) {
                    if (worksite.hasWrongLocationFlag) {
                        ExplainWrongLocationDialog(worksite)
                    }
                }

                Row(
                    Modifier
                        .testTag("editCasePropertyInfoJumpToCaseOnMap")
                        .clickable(onClick = onJumpToCaseOnMap)
                        .fillMaxWidth()
                        .padding(horizontal = edgeSpacing, vertical = edgeSpacingHalf),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = listItemSpacedBy,
                ) {
                    val actionDescription =
                        LocalAppTranslator.current.translate("actions.jump_to_case")
                    Image(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_jump_to_case_on_map),
                        contentDescription = actionDescription,
                    )

                    if (distanceAwayText.isNotBlank()) {
                        Text(distanceAwayText, style = MaterialTheme.typography.bodyLarge)
                    } else if (actionDescription?.isNotBlank() == true) {
                        Text(actionDescription, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Box(
                    Modifier
                        // TODO Common dimensions
                        .height(192.dp)
                        .padding(top = edgeSpacingHalf),
                ) {
                    PropertyInfoMapView(
                        worksite.coordinates,
                        isMapSatelliteView,
                        Modifier
                            .fillMaxSize()
                            .testTag("editCasePropertyInfoMapView"),
                        mapMarkerIcon = mapMarkerIcon,
                    )

                    MapViewToggleButton(
                        isMapSatelliteView,
                        setMapSatelliteView,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.workItems(
    workTypeProfile: WorkTypeProfile? = null,
    claimAll: () -> Unit = {},
    releaseAll: () -> Unit = {},
    requestAll: () -> Unit = {},
    updateWorkType: (WorkType, Boolean) -> Unit = { _, _ -> },
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
                val t = LocalAppTranslator.current
                val isEditable = LocalCaseEditor.current.isEditable
                if (profile.unclaimed.isNotEmpty()) {
                    WorkTypePrimaryAction(
                        t("actions.claim_all_alt"),
                        enabled = isEditable,
                        indicateBusy = !isEditable,
                        claimAll,
                    )
                }
                if (profile.releasableCount > 0) {
                    WorkTypeBusyAction(t("actions.release_all"), isEditable, releaseAll)
                } else if (profile.requestableCount > 0) {
                    WorkTypeBusyAction(t("actions.request_all"), isEditable, requestAll)
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

@Composable
private fun CasePhotosView(
    viewModel: ViewCaseViewModel = hiltViewModel(),
    setEnablePagerScroll: (Boolean) -> Unit = {},
    onPhotoSelect: (ViewImageArgs) -> Unit = { _ -> },
) {
    val photos by viewModel.beforeAfterPhotos.collectAsStateWithLifecycle()
    val syncingWorksiteImage by viewModel.syncingWorksiteImage.collectAsStateWithLifecycle()
    val deletingImageIds by viewModel.deletingImageIds.collectAsStateWithLifecycle()

    val onUpdateImageCategory = remember(viewModel) {
        { imageCategory: ImageCategory ->
            viewModel.addImageCategory = imageCategory
        }
    }

    val viewHeaderTitle by viewModel.headerTitle.collectAsStateWithLifecycle()
    CasePhotoImageView(
        viewModel,
        setEnablePagerScroll,
        photos,
        syncingWorksiteImage,
        deletingImageIds,
        onUpdateImageCategory,
        viewHeaderTitle,
        onPhotoSelect = onPhotoSelect,
    )
}

@Composable
private fun CaseNotesView(
    worksite: Worksite,
    viewModel: ViewCaseViewModel = hiltViewModel(),
) {
    val isEditable = LocalCaseEditor.current.isEditable
    val t = LocalAppTranslator.current

    var editingNote by remember { mutableStateOf("") }
    val saveNote = remember(viewModel) {
        { note: WorksiteNote -> viewModel.saveNote(note) }
    }

    val otherNotes by viewModel.otherNotes.collectAsStateWithLifecycle()
    val otherNotesLabel = t("caseView.other_notes")
    var hideOtherNotes by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val closeKeyboard = rememberCloseKeyboard(viewModel)

    val listState = rememberLazyListState()
    val isScrolledDown by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }
    ConstraintLayout {
        val (noteContent, newNoteFab) = createRefs()

        val notes = worksite.notes
        LazyColumn(
            modifier = Modifier
                .testTag("viewCaseNoteContents")
                .constrainAs(noteContent) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .fillMaxSize()
                .scrollFlingListener(closeKeyboard),
            state = listState,
            verticalArrangement = listItemSpacedBy,
        ) {
            item {
                Column(
                    listItemModifier,
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = listItemSpacedBy,
                ) {
                    CrisisCleanupTextArea(
                        text = editingNote,
                        onTextChange = { editingNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(t("caseView.note")) },
                        enabled = isEditable,
                        imeAction = ImeAction.Default,
                    )

                    CrisisCleanupButton(
                        onClick = {
                            val note = WorksiteNote.create().copy(
                                note = editingNote,
                            )
                            saveNote(note)
                            editingNote = ""
                            hideOtherNotes = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text = t("actions.add"),
                        enabled = isEditable && editingNote.isNotBlank(),
                    )
                }
            }

            if (otherNotes.isNotEmpty()) {
                item {
                    Row(
                        Modifier
                            .clickable {
                                hideOtherNotes = !hideOtherNotes
                            }
                            .then(listItemModifier),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(otherNotesLabel)
                        Spacer(Modifier.weight(1f))
                        CollapsibleIcon(
                            isCollapsed = hideOtherNotes,
                            sectionTitle = otherNotesLabel,
                        )
                    }
                }

                if (!hideOtherNotes) {
                    otherNoteItems(
                        otherNotes,
                        isCardView = true,
                    )
                }
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
                        .fabPlusSpaceHeight(),
                )
            }
        }

        val isKeyboardOpen = rememberIsKeyboardOpen()
        AnimatedVisibility(
            modifier = Modifier.constrainAs(newNoteFab) {
                end.linkTo(parent.end, margin = actionEdgeSpace)
                bottom.linkTo(parent.bottom, margin = actionEdgeSpace)
            },
            visible = !isKeyboardOpen,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CrisisCleanupFab(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.testTag("viewCaseAddNoteAction"),
                enabled = isScrolledDown,
            ) {
                Icon(
                    imageVector = CrisisCleanupIcons.CaretUp,
                    contentDescription = t("actions.scroll_to_top"),
                )
            }
        }
    }
}

data class IconTextAction(
    @DrawableRes val iconResId: Int = 0,
    val imageVector: ImageVector? = null,
    @StringRes val textResId: Int = 0,
    val translationKey: String = "",
)

@Composable
private fun PropertyInfoMapView(
    coordinates: LatLng,
    isSatelliteView: Boolean,
    modifier: Modifier = Modifier,
    mapMarkerIcon: BitmapDescriptor? = null,
    onMapLoaded: () -> Unit = {},
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
) {
    val uiSettings by rememberMapUiSettings(
        zoomControls = true,
        disablePanning = true,
    )

    val markerState = rememberUpdatedMarkerState(coordinates)

    val update = CameraUpdateFactory.newLatLngZoom(coordinates, 13f)
    cameraPositionState.move(update)

    var mapProperties by rememberMapProperties()
    LaunchedEffect(isSatelliteView) {
        val mapType = if (isSatelliteView) MapType.SATELLITE else MapType.NORMAL
        mapProperties = mapProperties.copy(mapType = mapType)
    }
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
