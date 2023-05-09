package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.LinkifyEmailText
import com.crisiscleanup.core.ui.LinkifyLocationText
import com.crisiscleanup.core.ui.LinkifyPhoneText
import com.crisiscleanup.feature.caseeditor.*
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

// TODO Use/move common dimensions
internal val edgeSpacing = 20.dp
internal val edgeSpacingHalf = edgeSpacing.times(0.5f)

@Composable
internal fun EditExistingCaseRoute(
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
) {
    val worksite by viewModel.worksite.collectAsStateWithLifecycle()
    val isEmptyWorksite = worksite == EmptyWorksite

    val toggleFavorite = remember(viewModel) { { viewModel.toggleFavorite() } }
    val toggleHighPriority = remember(viewModel) { { viewModel.toggleHighPriority() } }
    Column {
        val title by viewModel.headerTitle.collectAsStateWithLifecycle()
        val subTitle by viewModel.subTitle.collectAsStateWithLifecycle()
        TopBar(
            title,
            subTitle,
            isFavorite = worksite.isLocalFavorite,
            isHighPriority = worksite.hasHighPriorityFlag,
            onBack,
            isEmptyWorksite,
            toggleFavorite,
            toggleHighPriority,
        )

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
        } else {
            val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }

            val isSaving by viewModel.isSavingWorksite.collectAsStateWithLifecycle()
            // TODO Test
            val isEditable = !isSaving

            val statusOptions by viewModel.statusOptions.collectAsStateWithLifecycle()
            val caseEditor = CaseEditor(isEditable, statusOptions)
            CompositionLocalProvider(LocalCaseEditor provides caseEditor) {
                ExistingCaseContent(
                    worksite,
                    translate = translate,
                )
            }

            BottomActions(
                worksite,
                translate,
                onFullEdit,
            )
        }
    }
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
            stringResource(R.string.back),
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

            IconButton(
                onClick = toggleFavorite,
            ) {
                val iconResId = if (isFavorite) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
                val descriptionResId = if (isFavorite) R.string.not_favorite
                else R.string.favorite
                val tint = if (isFavorite) primaryRedColor
                else neutralIconColor
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = stringResource(descriptionResId),
                    tint = tint,
                )
            }
            IconButton(
                onClick = toggleHighPriority,
            ) {
                val descriptionResId = if (isHighPriority) R.string.not_high_priority
                else R.string.high_priority
                val tint = if (isHighPriority) primaryRedColor
                else neutralIconColor
                Icon(
                    painter = painterResource(R.drawable.ic_important_filled),
                    contentDescription = stringResource(descriptionResId),
                    tint = tint,
                )
            }
        }
    }
    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
        actions = actionsContent,
//        colors = TopAppBarDefaults.centerAlignedTopAppBarColors,
    )
}

// TODO Translate if available
private val tabTitles = listOf(
    R.string.info,
    R.string.photos,
    R.string.notes,
    R.string.release,
)

@Composable
private fun ColumnScope.ExistingCaseContent(
    worksite: Worksite,
    translate: (String) -> String = { s -> s },
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
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
        tabTitles.forEachIndexed { index, titleResId ->
            Tab(
                text = { Text(stringResource(titleResId)) },
                selected = selectedTabIndex == index,
                onClick = { selectedTabIndex = index },
            )
        }
    }

    Box(Modifier.weight(1f)) {
        when (selectedTabIndex) {
            0 -> EditExistingCaseInfoView(worksite, translate = translate)
            1 -> {}
            2 -> EditExistingCaseNotesView(worksite, translate = translate)
            3 -> {}
        }
    }
}

@Composable
private fun BottomActions(
    worksite: Worksite,
    translate: (String) -> String? = { null },
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
) {
    val contentColor = Color.Black
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

            NavigationBarItem(selected = false, onClick = {
                when (index) {
                    0 -> {}
                    1 -> {}
                    2 -> {}
                    3 -> {
                        onFullEdit(ExistingWorksiteIdentifier(worksite.incidentId, worksite.id))
                    }
                }
            }, icon = {
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
            }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = contentColor,
                unselectedTextColor = contentColor,
                indicatorColor = CrisisCleanupNavigationDefaults.navigationIndicatorColor(),
            )
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
            LinkifyPhoneText(text, style)
        } else if (isEmail) {
            LinkifyEmailText(text, style)
        } else if (isLocation) {
            LinkifyLocationText(text, locationQuery, style)
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
    itemInfoSectionHeader(4, translate("caseView.report"))
}

@Composable
internal fun BoxScope.EditExistingCaseNotesView(
    worksite: Worksite,
    viewModel: ExistingCaseViewModel = hiltViewModel(),
    translate: (String) -> String = { s -> s },
    isEditable: Boolean = false,
) {
    Text("Notes")
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
