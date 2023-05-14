package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import com.crisiscleanup.core.designsystem.component.actionEdgeSpace
import com.crisiscleanup.core.designsystem.component.fabPlusSpaceHeight
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
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
) {
    val isPendingTransfer by viewModel.transferWorkTypeProvider.isPendingTransfer
    if (isPendingTransfer) {
        openTransferWorkType()
    }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSavingWorksite.collectAsStateWithLifecycle()
    val isBusy = isLoading || isSaving
    val isEditable = !(isBusy || isPendingTransfer)

    val toggleFavorite = remember(viewModel) { { viewModel.toggleFavorite() } }
    val toggleHighPriority = remember(viewModel) { { viewModel.toggleHighPriority() } }
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
            viewModel.translate("actions.back"),
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
            val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }

            val statusOptions by viewModel.statusOptions.collectAsStateWithLifecycle()
            val caseEditor = CaseEditor(isEditable, statusOptions)
            CompositionLocalProvider(LocalCaseEditor provides caseEditor) {
                ExistingCaseContent(
                    tabTitles,
                    worksite,
                    translate,
                    isBusy,
                )

                BottomActions(
                    worksite,
                    translate,
                    onFullEdit,
                )
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
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
    backText: String,
) {
    // TODO Style components as necessary

    val titleContent = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        Text(
            backText,
            Modifier
                .clickable(onClick = onBack)
                .padding(8.dp),
        )
    }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (isLoading) {
        @Composable {}
    } else {
        @Composable {
            // TODO Translations if exist

            val iconResId = if (isFavorite) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
            val favoriteResId = if (isFavorite) R.string.not_favorite
            else R.string.favorite
            val favoriteTint = getTopIconActionColor(isFavorite, isEditable)
            CrisisCleanupIconButton(
                iconResId = iconResId,
                contentDescriptionResId = favoriteResId,
                onClick = toggleFavorite,
                enabled = isEditable,
                tint = favoriteTint,
            )

            val highPriorityResId = if (isHighPriority) R.string.not_high_priority
            else R.string.high_priority
            val highPriorityTint = getTopIconActionColor(isHighPriority, isEditable)
            CrisisCleanupIconButton(
                iconResId = R.drawable.ic_important_filled,
                contentDescriptionResId = highPriorityResId,
                onClick = toggleHighPriority,
                enabled = isEditable,
                tint = highPriorityTint,
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
    translate: (String) -> String = { s -> s },
    isLoading: Boolean = false,
) {
    val pagerState = rememberPagerState()
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

    Box(Modifier.weight(1f)) {
        HorizontalPager(
            pageCount = tabTitles.size,
            state = pagerState,
        ) { pagerIndex ->
            when (pagerIndex) {
                0 -> EditExistingCaseInfoView(worksite, translate = translate)
                1 -> EditExistingCasePhotosView(worksite, translate = translate)
                2 -> EditExistingCaseNotesView(worksite, translate = translate)
            }
        }
        BusyIndicatorFloatingTopCenter(isLoading)
    }
}

@Composable
private fun BottomActions(
    worksite: Worksite,
    translate: (String) -> String? = { null },
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
) {
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
            var label = action.text
            if (action.translationKey.isNotBlank()) {
                translate(action.translationKey)?.let {
                    label = it
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
            LinkifyPhoneText(text, style = style)
        } else if (isEmail) {
            LinkifyEmailText(text, style = style)
        } else if (isLocation) {
            LinkifyLocationText(text, locationQuery, style = style)
        } else {
            Text(text, style = style)
        }
    }
}

@Composable
internal fun EditExistingCaseInfoView(
    worksite: Worksite,
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    translate: (String) -> String = { s -> s },
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
            val worksiteData by viewModel.worksiteData.collectAsStateWithLifecycle()
            worksiteData?.let { uiData ->
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                CaseIncident(
                    Modifier,
                    uiData.incident,
                    uiData.isPendingSync,
                    isSyncing = isSyncing,
                )
            }
        }

        propertyInfoItems(worksite, translate, mapMarkerIcon)
        workItems(
            translate,
            workTypeProfile,
            claimAll = claimAll,
            requestAll = requestAll,
            releaseAll = releaseAll,
            updateWorkType = updateWorkType,
            requestWorkType = requestWorkType,
            releaseWorkType = releaseWorkType,
        )
        volunteerReportItems(worksite, translate)

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
    title: String,
    trailingContent: (@Composable () -> Unit)? = null,
) = item(
    "section-header-$sectionIndex",
    "content-header-$sectionIndex",
) {
    SectionHeader(
        Modifier.padding(top = if (sectionIndex > 0) edgeSpacing else 0.dp),
        sectionIndex,
        title,
        trailingContent,
    )
}

private fun LazyListScope.propertyInfoItems(
    worksite: Worksite,
    translate: (String) -> String = { s -> s },
    mapMarkerIcon: BitmapDescriptor? = null,
) {
    itemInfoSectionHeader(0, translate("caseForm.property_information"))

    item(key = "section-content-property") {
        val rowItemModifier = Modifier.padding(horizontal = edgeSpacing)
        CardSurface {
            Column(
                verticalArrangement = Arrangement.spacedBy(edgeSpacing),
            ) {
                PropertyInfoRow(
                    CrisisCleanupIcons.Person,
                    worksite.name,
                    rowItemModifier.padding(top = edgeSpacing),
                )
                PropertyInfoRow(
                    CrisisCleanupIcons.Phone,
                    listOf(worksite.phone1, worksite.phone2).filterNotBlankTrim()
                        .joinToString("; "),
                    rowItemModifier,
                    isPhone = true,
                )
                worksite.email?.let {
                    if (it.isNotBlank()) {
                        PropertyInfoRow(
                            CrisisCleanupIcons.Mail,
                            it,
                            rowItemModifier,
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
                    rowItemModifier,
                    isLocation = !worksite.hasWrongLocationFlag,
                    locationQuery = locationQuery,
                )

                PropertyInfoMapView(
                    worksite.coordinates(),
                    // TODO Common dimensions
                    Modifier.height(192.dp),
                    mapMarkerIcon = mapMarkerIcon,
                )
            }
        }
    }
}

private fun LazyListScope.workItems(
    translate: (String) -> String = { s -> s },
    workTypeProfile: WorkTypeProfile? = null,
    claimAll: () -> Unit = {},
    releaseAll: () -> Unit = {},
    requestAll: () -> Unit = {},
    updateWorkType: (WorkType) -> Unit = {},
    requestWorkType: (WorkType) -> Unit = {},
    releaseWorkType: (WorkType) -> Unit = {},
) {
    workTypeProfile?.let { profile ->
        itemInfoSectionHeader(2, translate("caseForm.work")) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(edgeSpacingHalf),
            ) {
                if (profile.unclaimed.isNotEmpty()) {
                    WorkTypeAction(translate("actions.claim_all_alt"), claimAll)
                }
                if (profile.releasableCount > 0) {
                    WorkTypeAction(translate("actions.release_all"), releaseAll)
                } else if (profile.requestableCount > 0) {
                    WorkTypeAction(translate("actions.request_all"), requestAll)
                }
            }
        }

        val rowItemModifier = Modifier.padding(horizontal = edgeSpacing)

        if (profile.otherOrgClaims.isNotEmpty()) {
            profile.otherOrgClaims.forEach { otherOrgClaim ->
                organizationWorkClaims(
                    otherOrgClaim,
                    rowItemModifier,
                    translate,
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
                translate,
                updateWorkType,
                requestWorkType,
                releaseWorkType,
            )
        }

        if (profile.unclaimed.isNotEmpty()) {
            val unclaimedTitle = translate("caseView.unclaimed_work_types")
            workTypeSectionTitle(unclaimedTitle, "unclaimed")
            existingWorkTypeItems(
                "unclaimed",
                profile.unclaimed,
                rowItemModifier,
                translate,
                updateWorkType,
                requestWorkType,
                releaseWorkType,
            )
        }
    }
}

private fun LazyListScope.volunteerReportItems(
    worksite: Worksite,
    translate: (String) -> String = { s -> s },
) {
    // itemInfoSectionHeader(4, translate("caseView.report"))
}

@Composable
internal fun EditExistingCasePhotosView(
    worksite: Worksite,
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    translate: (String) -> String = { s -> s },
    isEditable: Boolean = false,
) {
    Text(
        "Photos",
        Modifier.fillMaxSize(),
    )
}

@Composable
internal fun EditExistingCaseNotesView(
    worksite: Worksite,
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    translate: (String) -> String = { s -> s },
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
        LazyColumn(state = listState) {
            staticNoteItems(
                notes,
                notes.size,
                listItemModifier,
            )
            item {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .fabPlusSpaceHeight()
                )
            }
        }

        FloatingActionButton(
            onClick = onAddNote,
            modifier = Modifier.constrainAs(newNoteFab) {
                end.linkTo(parent.end, margin = actionEdgeSpace)
                bottom.linkTo(parent.bottom, margin = actionEdgeSpace)
            },
            shape = CircleShape,
        ) {
            Icon(
                imageVector = CrisisCleanupIcons.AddNote,
                contentDescription = viewModel.translate("caseView.add_note_alt"),
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
        OnCreateNote(translate, saveNote, dismissNoteDialog)
    }
}

data class IconTextAction(
    @DrawableRes val iconResId: Int = 0,
    val imageVector: ImageVector? = null,
    @StringRes val textResId: Int = 0,
    val text: String = "",
    val translationKey: String = "",
)

private val existingCaseActions = listOf(
    IconTextAction(
        iconResId = R.drawable.ic_share_small,
        textResId = R.string.share,
    ),
    IconTextAction(
        iconResId = R.drawable.ic_flag_small,
        textResId = R.string.flag,
    ),
    IconTextAction(
        iconResId = R.drawable.ic_history_small,
        textResId = R.string.history,
    ),
    IconTextAction(
        iconResId = R.drawable.ic_edit_underscore_small,
        textResId = R.string.edit,
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
