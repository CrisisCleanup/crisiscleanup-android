package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.mapmarker.ui.rememberMapProperties
import com.crisiscleanup.core.mapmarker.ui.rememberMapUiSettings
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.*
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.model.coordinates
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

private val cardContainerColor = Color.White

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
            isFavorite = worksite.isFavorited,
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
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
        )
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

    LazyColumn {
        propertyInfoItems(worksite, translate, mapMarkerIcon)
        workItems(
            worksite,
            translate,
            workTypeProfile,
            claimAll = claimAll,
            requestAll = requestAll,
            releaseAll = releaseAll,
            updateWorkType = updateWorkType,
            requestWorkType = requestWorkType,
        )
        volunteerReportItems(worksite, translate)
    }
}

// TODO Use/move common dimensions
private val edgeSpacing = 20.dp
private val edgeSpacingHalf = edgeSpacing.times(0.5f)

@Composable
private fun CardSurface(
    cornerRound: Dp = 4.dp,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .listItemHorizontalPadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(cornerRound),
        color = cardContainerColor,
        tonalElevation = 0.dp,
        shadowElevation = elevation,
    ) {
        content()
    }
}

@Composable
private fun InfoSectionHeader(
    sectionIndex: Int,
    title: String,
    trailingContent: (@Composable () -> Unit)? = null,
) = SectionHeader(
    Modifier.padding(top = if (sectionIndex > 0) edgeSpacing else 0.dp),
    sectionIndex,
    title,
    trailingContent,
)

private fun LazyListScope.propertyInfoItems(
    worksite: Worksite,
    translate: (String) -> String = { s -> s },
    mapMarkerIcon: BitmapDescriptor? = null,
) {
    item(key = "section-header-property") {
        InfoSectionHeader(0, translate("caseForm.property_information"))
    }
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
                )
                worksite.email?.let {
                    PropertyInfoRow(
                        CrisisCleanupIcons.Mail,
                        it,
                        rowItemModifier,
                    )
                }
                PropertyInfoRow(
                    CrisisCleanupIcons.Location,
                    listOf(
                        worksite.address,
                        worksite.city,
                        worksite.state,
                        worksite.postalCode,
                    ).combineTrimText(),
                    rowItemModifier,
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

@Composable
private fun ClaimingOrganization(
    name: String,
    isMyOrganization: Boolean,
    modifier: Modifier = Modifier,
    translate: (String) -> String = { s -> s },
) {
    if (isMyOrganization) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(edgeSpacingHalf),
        ) {
            Text(name)
            val myOrganizationLabel = translate("profileUser.your_organization")
            Text(
                "($myOrganizationLabel)",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    } else {
        Text(name, modifier)
    }
}

@Composable
private fun WorkTypeOrgClaims(
    isMyOrgClaim: Boolean,
    myOrgName: String,
    translate: (String) -> String,
    otherOrgClaims: List<String>,
    rowItemModifier: Modifier = Modifier,
) {
    Text(
        translate("caseView.claimed_by"),
        rowItemModifier,
        style = MaterialTheme.typography.bodySmall,
    )
    Column(
        Modifier.padding(bottom = edgeSpacing),
        verticalArrangement = Arrangement.spacedBy(
            edgeSpacingHalf
        ),
    ) {
        if (isMyOrgClaim) {
            ClaimingOrganization(myOrgName, true, rowItemModifier, translate)
        }
        otherOrgClaims.forEach { otherOrganization ->
            ClaimingOrganization(otherOrganization, false, rowItemModifier)
        }
    }
}

@Composable
private fun WorkTypeSummaryView(
    summary: WorkTypeSummary,
    rowItemModifier: Modifier = Modifier,
    translate: (String) -> String = { s -> s },
    updateWorkType: (WorkType) -> Unit = {},
    requestWorkType: (WorkType) -> Unit = {},
) {
    val updateWorkTypeStatus = remember(updateWorkType) {
        { status: WorkTypeStatus ->
            updateWorkType(summary.workType.copy(statusLiteral = status.literal))
        }
    }
    CardSurface(elevation = 1.dp) {
        Column {
            Text(
                summary.name,
                rowItemModifier.padding(top = edgeSpacing),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (summary.jobSummary.isNotBlank()) {
                Text(
                    summary.jobSummary,
                    rowItemModifier.padding(top = edgeSpacingHalf),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            // TODO Row of actions with callbacks

            Row(
                modifier = rowItemModifier.listItemVerticalPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkTypeStatusDropdown(summary.workType.status, updateWorkTypeStatus, translate)
                Spacer(Modifier.weight(1f))

                val isEditable = LocalCaseEditor.current.isEditable

                if (summary.workType.isClaimed) {
                    if (summary.isClaimedByMyOrg) {
                        WorkTypeAction(translate("actions.unclaim"), isEditable) {
                            updateWorkType(summary.workType.copy(orgClaim = null))
                        }
                    } else {
                        WorkTypeAction(translate("actions.request"), isEditable) {
                            requestWorkType(summary.workType)
                        }
                    }
                } else {
                    WorkTypeAction(translate("actions.claim"), isEditable) {
                        updateWorkType(summary.workType.copy(orgClaim = summary.myOrgId))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkTypeAction(
    text: String,
    isEditable: Boolean,
    onClick: () -> Unit = {},
) = CrisisCleanupOutlinedButton(
    // TODO Common dimensions
    modifier = Modifier.widthIn(100.dp),
    text = text,
    onClick = onClick,
    enabled = isEditable,
)

private fun getWorkTypeAllActions(
    workTypeProfile: WorkTypeProfile?,
    translate: (String) -> String,
    claimAll: () -> Unit = {},
    releaseAll: () -> Unit = {},
    requestAll: () -> Unit = {},
): (@Composable () -> Unit)? =
    workTypeProfile?.let {
        {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(edgeSpacingHalf),
            ) {
                val isEditable = LocalCaseEditor.current.isEditable
                // TODO actions.release_all
                if (it.unclaimedCount > 0) {
                    WorkTypeAction(translate("actions.claim_all_alt"), isEditable, claimAll)
                }
                if (it.requestCount > 0) {
                    WorkTypeAction(translate("actions.request_all"), isEditable, requestAll)
                }
            }
        }
    }

private fun LazyListScope.workItems(
    worksite: Worksite,
    translate: (String) -> String = { s -> s },
    workTypeProfile: WorkTypeProfile? = null,
    claimAll: () -> Unit = {},
    releaseAll: () -> Unit = {},
    requestAll: () -> Unit = {},
    updateWorkType: (WorkType) -> Unit = {},
    requestWorkType: (WorkType) -> Unit = {},
) {
    item(key = "section-header-work") {
        val trailingActions = getWorkTypeAllActions(
            workTypeProfile,
            translate,
            claimAll,
            releaseAll,
            requestAll,
        )
        InfoSectionHeader(
            2,
            translate("caseForm.work"),
            trailingActions,
        )
    }

    val rowItemModifier = Modifier.padding(horizontal = edgeSpacing)
    workTypeProfile?.let { profile ->
        with(profile.orgClaims) {
            if (isMyOrgClaim || otherOrgClaims.isNotEmpty()) {
                item("section-claimed-by") {
                    WorkTypeOrgClaims(
                        isMyOrgClaim,
                        myOrgName,
                        translate,
                        otherOrgClaims,
                        rowItemModifier,
                    )
                }
            }
        }

        profile.summaries.forEachIndexed { index, workTypeSummary ->
            if (index > 0) {
                item("section-work-type-summary-spacer-$index") {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                }
            }

            item("section-work-type-summary-$index") {
                WorkTypeSummaryView(
                    workTypeSummary,
                    rowItemModifier,
                    translate,
                    updateWorkType,
                    requestWorkType,
                )
            }
        }
    }
}

private fun LazyListScope.volunteerReportItems(
    worksite: Worksite,
    translate: (String) -> String = { s -> s },
) {
    item(key = "section-header-report") {
        InfoSectionHeader(4, translate("caseView.report"))
    }
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

@Preview
@Composable
private fun OrgClaimsPreview() {
    val otherOrgClaims = listOf(
        "Team green",
        "True blue",
        "Soarin Orange",
    )
    Column {
        WorkTypeOrgClaims(
            true,
            "My organization",
            { key ->
                when (key) {
                    "profileUser.your_organization" -> "My org"
                    else -> "Claimed by"
                }
            },
            otherOrgClaims,
        )
    }
}