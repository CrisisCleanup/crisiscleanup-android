package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appnav.ViewImageArgs
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.common.urlEncode
import com.crisiscleanup.core.commoncase.model.addressQuery
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.CrisisCleanupFab
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import com.crisiscleanup.core.designsystem.component.LinkifyEmailText
import com.crisiscleanup.core.designsystem.component.LinkifyLocationText
import com.crisiscleanup.core.designsystem.component.LinkifyPhoneText
import com.crisiscleanup.core.designsystem.component.TemporaryDialog
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.component.WorkTypeAction
import com.crisiscleanup.core.designsystem.component.WorkTypePrimaryAction
import com.crisiscleanup.core.designsystem.component.actionEdgeSpace
import com.crisiscleanup.core.designsystem.component.fabPlusSpaceHeight
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.cardContainerColor
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
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
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.core.model.data.WorksiteNote
import com.crisiscleanup.core.ui.ScreenKeyboardVisibility
import com.crisiscleanup.core.ui.screenKeyboardVisibility
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
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
    openTransferWorkType: () -> Unit = {},
    openPhoto: (ViewImageArgs) -> Unit = { _ -> },
    openAddFlag: () -> Unit = {},
    openShareCase: () -> Unit = {},
    openCaseHistory: () -> Unit = {},
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

            val translator: KeyResourceTranslator = viewModel
            val tabTitles by viewModel.tabTitles.collectAsStateWithLifecycle()

            val updatedAtText by viewModel.updatedAtText.collectAsStateWithLifecycle()
            if (updatedAtText.isNotBlank()) {
                Text(
                    updatedAtText,
                    Modifier
                        .testTag("editCaseUpdatedAtText")
                        .background(Color.White)
                        .then(listItemModifier),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (isEmptyWorksite) {
                if (viewModel.worksiteIdArg == EmptyWorksite.id) {
                    Text(
                        translator("info.no_worksite_selected"),
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
                CompositionLocalProvider(
                    LocalCaseEditor provides caseEditor,
                    LocalAppTranslator provides translator,
                ) {
                    ExistingCaseContent(
                        tabTitles,
                        worksite,
                        isBusy,
                        openPhoto,
                        copyToClipboard,
                    )
                    val keyboardVisibility by screenKeyboardVisibility()
                    if (keyboardVisibility == ScreenKeyboardVisibility.NotVisible) {
                        BottomActions(
                            worksite,
                            onFullEdit,
                            onCaseFlags = openAddFlag,
                            onCaseShare = openShareCase,
                            onCaseHistory = openCaseHistory,
                        )
                    }
                }
            }
        }

        CopiedToClipboard(clipboardContents)

        val actionDescription by viewModel.actionDescriptionMessage.collectAsStateWithLifecycle()
        TemporaryDialog(actionDescription)
    }
}

private fun getTopIconActionColor(
    isActive: Boolean,
    isEditable: Boolean,
): Color {
    var tint = if (isActive) {
        primaryRedColor
    } else {
        neutralIconColor
    }
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
    val titleContent = @Composable {
        Column(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onCaseLongPress,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = LocalFontStyles.current.header3,
                modifier = Modifier.testTag("editCaseHeaderText"),
            )

            if (subTitle.isNotBlank()) {
                Text(
                    subTitle,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("editCaseSubHeaderText"),
                )
            }
        }
    }

    val navigationContent = @Composable {
        TopBarBackAction(action = onBack, modifier = Modifier.testTag("topBarBackAction"))
    }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (isLoading) {
        @Composable {}
    } else {
        @Composable {
            val translator = LocalAppTranslator.current
            val highPriorityTranslateKey =
                if (isHighPriority) {
                    "actions.unmark_high_priority"
                } else {
                    "flag.flag_high_priority"
                }
            val highPriorityTint = getTopIconActionColor(isHighPriority, isEditable)
            CrisisCleanupIconButton(
                iconResId = R.drawable.ic_important_filled,
                contentDescription = translator(highPriorityTranslateKey),
                onClick = toggleHighPriority,
                enabled = isEditable,
                tint = highPriorityTint,
                modifier = Modifier.testTag("editCaseHighPriorityToggleBtn"),
            )

            val iconResId = if (isFavorite) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart_outline
            }
            val favoriteDescription =
                if (isFavorite) {
                    translator("actions.not_member_of_my_org")
                } else {
                    translator("actions.member_of_my_org")
                }
            val favoriteTint = getTopIconActionColor(isFavorite, isEditable)
            CrisisCleanupIconButton(
                iconResId = iconResId,
                contentDescription = favoriteDescription,
                onClick = toggleFavorite,
                enabled = isEditable,
                tint = favoriteTint,
                modifier = Modifier.testTag("editCaseFavoriteToggleBtn"),
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
    onCaseFlags: () -> Unit = {},
    onCaseShare: () -> Unit = {},
    onCaseHistory: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
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
                modifier = Modifier.testTag("editCaseNavItem_$label"),
                selected = false,
                onClick = {
                    when (index) {
                        0 -> onCaseShare()
                        1 -> onCaseFlags()
                        2 -> onCaseHistory()
                        3 -> onFullEdit(
                            ExistingWorksiteIdentifier(
                                worksite.incidentId,
                                worksite.id,
                            ),
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
                label = {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
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
internal fun PropertyInfoRow(
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

    val removeFlag = remember(viewModel) { { flag: WorksiteFlag -> viewModel.removeFlag(flag) } }

    val claimAll = remember(viewModel) { { viewModel.claimAll() } }
    val requestAll = remember(viewModel) { { viewModel.requestAll() } }
    val releaseAll = remember(viewModel) { { viewModel.releaseAll() } }
    val updateWorkType =
        remember(viewModel) {
            { updated: WorkType, isStatusChange: Boolean ->
                viewModel.updateWorkType(updated, isStatusChange)
            }
        }
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

        flagItems(worksite, removeFlag)
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
        val description = translator(removeFlagTranslateKey)
            .replace("{flag}", text)

        var contentColor = Color.White
        if (!isEditable) {
            contentColor = contentColor.disabledAlpha()
        }

        AssistChip(
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 28.dp, minHeight = 40.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = isEditable,
                            onClick = { removeFlag(flag) },
                            role = Role.Button,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = CrisisCleanupIcons.Clear,
                        contentDescription = description,
                        tint = contentColor,
                    )
                }
            },
            label = {
                Text(
                    text,
                    Modifier.padding(end = 2.dp),
                )
            },
            shape = CircleShape,
            border = null,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = color,
                labelColor = contentColor,
            ),
            onClick = {},
        )
    }
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
                        .testTag("editCasePropertyInfoWorksiteNameRow")
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { copyToClipboard(worksite.name) },
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
                        .testTag("editCasePropertyInfoPhoneRow")
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { copyToClipboard(phoneNumbers) },
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
                    isLocation = !worksite.hasWrongLocationFlag,
                    locationQuery = geoQuery.ifBlank { locationQuery },
                )

                PropertyInfoMapView(
                    worksite.coordinates,
                    // TODO Common dimensions
                    Modifier
                        .testTag("editCasePropertyInfoMapView")
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
                    WorkTypePrimaryAction(t("actions.claim_all_alt"), isEditable, claimAll)
                }
                if (profile.releasableCount > 0) {
                    WorkTypeAction(t("actions.release_all"), isEditable, releaseAll)
                } else if (profile.requestableCount > 0) {
                    WorkTypeAction(t("actions.request_all"), isEditable, requestAll)
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
internal fun EditExistingCasePhotosView(
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    setEnablePagerScroll: (Boolean) -> Unit = {},
    onPhotoSelect: (ViewImageArgs) -> Unit = { _ -> },
) {
    val photos by viewModel.beforeAfterPhotos.collectAsStateWithLifecycle()
    val syncingWorksiteImage by viewModel.syncingWorksiteImage.collectAsStateWithLifecycle()

    var showCameraMediaSelect by remember { mutableStateOf(false) }

    val translator = LocalAppTranslator.current
    val sectionTitleResIds = mapOf(
        ImageCategory.Before to translator("caseForm.before_photos"),
        ImageCategory.After to translator("caseForm.after_photos"),
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
                        .height(8.dp),
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
                        .fabPlusSpaceHeight(),
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
                contentDescription = LocalAppTranslator.current("caseView.add_note_alt"),
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

private val existingCaseActions = listOf(
    IconTextAction(
        iconResId = R.drawable.ic_share_small,
        translationKey = "actions.share",
    ),
    IconTextAction(
        iconResId = R.drawable.ic_flag_small,
        translationKey = "nav.flag",
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
