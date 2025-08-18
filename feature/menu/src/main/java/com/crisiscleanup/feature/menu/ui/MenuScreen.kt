package com.crisiscleanup.feature.menu.ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appcomponent.ui.AppTopBar
import com.crisiscleanup.core.common.TutorialStep
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.HotlineHeaderView
import com.crisiscleanup.core.designsystem.component.HotlineIncidentView
import com.crisiscleanup.core.designsystem.component.OpenSettingsDialog
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.component.actionRoundCornerShape
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.cardContainerColor
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.neutralFontColor
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentWorksitesCachePreferences
import com.crisiscleanup.core.model.data.TutorialViewId
import com.crisiscleanup.core.selectincident.SelectIncidentDialog
import com.crisiscleanup.core.ui.sizePosition
import com.crisiscleanup.feature.menu.MenuViewModel
import com.crisiscleanup.feature.menu.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
internal fun MenuRoute(
    viewModel: MenuViewModel,
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openRequestRedeploy: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openLists: () -> Unit = {},
    openIncidentCache: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    MenuScreen(
        openAuthentication = openAuthentication,
        openInviteTeammate = openInviteTeammate,
        openRequestRedeploy = openRequestRedeploy,
        openUserFeedback = openUserFeedback,
        openLists = openLists,
        openIncidentCache = openIncidentCache,
        openSyncLogs = openSyncLogs,
        viewModel = viewModel,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MenuScreen(
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openRequestRedeploy: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openLists: () -> Unit = {},
    openIncidentCache: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
    viewModel: MenuViewModel,
) {
    val t = LocalAppTranslator.current
    val translationCount by t.translationCount.collectAsStateWithLifecycle()

    val incidentsData by viewModel.incidentsData.collectAsStateWithLifecycle()

    var showIncidentPicker by remember { mutableStateOf(false) }
    val openIncidentsSelect = remember(viewModel) {
        { showIncidentPicker = true }
    }

    val isAppUpdateAvailable by viewModel.isAppUpdateAvailable.collectAsStateWithLifecycle(false)

    val isSyncPhotosImmediate by viewModel.isSyncPhotosImmediate.collectAsStateWithLifecycle(false)

    val isSharingAnalytics by viewModel.isSharingAnalytics.collectAsStateWithLifecycle(false)

    val isSharingLocation by viewModel.isSharingLocation.collectAsStateWithLifecycle(false)
    val locationPermission = rememberPermissionState(ACCESS_COARSE_LOCATION)
    var explainLocationRequest by remember { mutableStateOf(false) }
    val onShareLocation = remember(locationPermission) {
        { b: Boolean ->
            if (b && !locationPermission.status.isGranted) {
                with(locationPermission.status) {
                    if (shouldShowRationale) {
                        explainLocationRequest = true
                    } else {
                        locationPermission.launchPermissionRequest()
                    }
                }
            } else {
                viewModel.shareLocationWithOrg(b)
            }
        }
    }

    val menuItemVisibility by viewModel.menuItemVisibility.collectAsStateWithLifecycle()
    val isMenuTutorialDone by viewModel.isMenuTutorialDone.collectAsStateWithLifecycle(true)
    val tutorialViewLookup = viewModel.tutorialViewTracker.viewSizePositionLookup

    val isGettingStartedVisible =
        menuItemVisibility.showGettingStartedVideo || viewModel.isNotProduction

    val tutorialStep by viewModel.menuTutorialDirector.tutorialStep.collectAsStateWithLifecycle()
    val incidentDropdownModifier = Modifier.onGloballyPositioned { coordinates ->
        tutorialViewLookup[TutorialViewId.IncidentSelectDropdown] = coordinates.sizePosition
    }
    val accountToggleModifier = Modifier.onGloballyPositioned { coordinates ->
        tutorialViewLookup[TutorialViewId.AccountToggle] = coordinates.sizePosition
    }
    val inviteTeammateModifier = Modifier.onGloballyPositioned { coordinates ->
        tutorialViewLookup[TutorialViewId.InviteTeammate] = coordinates.sizePosition
    }
    val provideFeedbackModifier = Modifier.onGloballyPositioned { coordinates ->
        tutorialViewLookup[TutorialViewId.ProvideFeedback] = coordinates.sizePosition
    }

    var expandHotline by remember { mutableStateOf(false) }
    val toggleExpandHotline = { expandHotline = !expandHotline }

    val hotlineIncidents by viewModel.hotlineIncidents.collectAsStateWithLifecycle()
    val tutorialItemOffset = remember(hotlineIncidents, expandHotline) {
        val incidentRows = if (expandHotline) {
            hotlineIncidents.size
        } else {
            0
        }
        val headerSpacerCount = if (hotlineIncidents.isEmpty()) {
            0
        } else {
            3
        }
        incidentRows + headerSpacerCount
    }

    val incidentCachePreferences by viewModel.incidentCachePreferences.collectAsStateWithLifecycle()
    val incidentDataCacheMetrics by viewModel.incidentDataCacheMetrics.collectAsStateWithLifecycle()
    val hasSpeedNotAdaptive = incidentDataCacheMetrics.hasSpeedNotAdaptive

    Column {
        AppTopBar(
            incidentDropdownModifier = incidentDropdownModifier,
            accountToggleModifier = accountToggleModifier
                .testTag("menuAccountToggle"),
            incidentSelectTestTag = "menuIncidentSelect",
            dataProvider = viewModel.appTopBarDataProvider,
            openAuthentication = openAuthentication,
            onOpenIncidents = openIncidentsSelect,
        )

        val lazyListState = rememberLazyListState()
        val firstVisibleItemIndex by remember {
            derivedStateOf {
                lazyListState.layoutInfo.visibleItemsInfo[0].index
            }
        }
        val lastVisibleItemIndex by remember {
            derivedStateOf {
                lazyListState.layoutInfo.visibleItemsInfo.last().index
            }
        }
        val focusItemScrollOffset = (-72 * LocalDensity.current.density).toInt()
        LaunchedEffect(tutorialStep) {
            fun getListItemIndex(itemIndex: Int): Int {
                var listItemIndex = itemIndex + tutorialItemOffset
                if (!isMenuTutorialDone) {
                    listItemIndex += 1
                }
                if (isGettingStartedVisible) {
                    listItemIndex += 1
                }
                return listItemIndex
            }

            suspend fun scrollToListItem(itemIndex: Int) {
                val listItemIndex = getListItemIndex(itemIndex)
                if (firstVisibleItemIndex > listItemIndex - 2 ||
                    lastVisibleItemIndex < listItemIndex + 2
                ) {
                    lazyListState.scrollToItem(listItemIndex, focusItemScrollOffset)
                }
            }
            when (tutorialStep) {
                TutorialStep.MenuStart,
                TutorialStep.InviteTeammates,
                -> scrollToListItem(2)

                TutorialStep.ProvideAppFeedback -> scrollToListItem(4)

                else -> {}
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            state = lazyListState,
        ) {
            hotlineItems(
                hotlineIncidents,
                expandHotline,
                toggleExpandHotline,
            )

            if (!isMenuTutorialDone) {
                item {
                    MenuTutorial(
                        false,
                        viewModel.menuTutorialDirector::startTutorial,
                        viewModel::setMenuTutorialDone,
                        viewModel.isNotProduction,
                    )
                }
            }

            if (isGettingStartedVisible) {
                item {
                    val hideGettingStartedVideo = remember(viewModel) {
                        { viewModel.showGettingStartedVideo(false) }
                    }
                    GettingStartedSection(
                        menuItemVisibility.showGettingStartedVideo,
                        hideGettingStartedVideo = hideGettingStartedVideo,
                        viewModel.gettingStartedVideoUrl,
                        viewModel.isNotProduction,
                        toggleGettingStartedSection = viewModel::showGettingStartedVideo,
                    )
                }
            }

            if (isAppUpdateAvailable) {
                item {
                    AppUpdateView()
                }
            }

            item {
                IncidentCacheView(
                    incidentCachePreferences,
                    hasSpeedNotAdaptive,
                    openIncidentCache,
                    listItemModifier,
                )
            }

            item(
                key = "lists-item",
                contentType = "outline-button",
            ) {
                CrisisCleanupOutlinedButton(
                    modifier = listItemModifier.actionHeight(),
                    text = t("list.lists"),
                    onClick = openLists,
                    enabled = true,
                )
            }

            item(
                key = "invite-teammate-item",
                contentType = "primary-button",
            ) {
                val inviteUserText = remember(translationCount) {
                    t("usersVue.invite_new_user")
                }
                CrisisCleanupButton(
                    modifier = inviteTeammateModifier
                        .fillMaxWidth()
                        .listItemPadding(),
                    text = inviteUserText,
                    onClick = openInviteTeammate,
                )
            }

            item(
                key = "redeploy-item",
                contentType = "outline-button",
            ) {
                CrisisCleanupOutlinedButton(
                    modifier = listItemModifier.actionHeight(),
                    text = t("requestRedeploy.request_redeploy"),
                    onClick = openRequestRedeploy,
                    enabled = true,
                )
            }

            item(
                key = "feedback-item",
                contentType = "outline-button",
            ) {
                CrisisCleanupOutlinedButton(
                    modifier = provideFeedbackModifier
                        .fillMaxWidth()
                        .listItemPadding()
                        .actionHeight(),
                    text = t("info.give_app_feedback"),
                    onClick = openUserFeedback,
                    enabled = true,
                )
            }

            if (isMenuTutorialDone) {
                item {
                    val unsetMenuTutorialDone =
                        remember(viewModel) { { viewModel.setMenuTutorialDone(false) } }
                    MenuTutorial(
                        true,
                        viewModel.menuTutorialDirector::startTutorial,
                        unsetMenuTutorialDone,
                        viewModel.isNotProduction,
                    )
                }
            }

            item {
                Text(
                    viewModel.versionText,
                    listItemModifier,
                    color = neutralFontColor,
                )
            }

            toggleItem(
                "~~Sync photos immediately",
                isSyncPhotosImmediate,
                viewModel::syncPhotosImmediately,
            )

            toggleItem(
                "actions.share_analytics",
                isSharingAnalytics,
                viewModel::shareAnalytics,
            )

            toggleItem(
                "appMenu.share_location_with_organization",
                isSharingLocation,
                onShareLocation,
            )

            item {
                TermsPrivacyView(
                    termsOfServiceUrl = viewModel.termsOfServiceUrl,
                    privacyPolicyUrl = viewModel.privacyPolicyUrl,
                )
            }

            if (viewModel.isDebuggable) {
                item {
                    MenuScreenNonProductionView(viewModel)
                }
            }

            if (viewModel.isNotProduction) {
                item {
                    CrisisCleanupTextButton(
                        onClick = openSyncLogs,
                        text = "See sync logs",
                    )
                }

                item {
                    CrisisCleanupTextButton(
                        onClick = viewModel::checkInactivity,
                        text = "Check inactivity",
                    )
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        CrisisCleanupTextButton(
                            onClick = viewModel::clearAppData,
                            text = "Clear app data",
                        )
                    }
                }
            }
        }
    }

    if (showIncidentPicker) {
        val isLoadingIncidents by viewModel.isLoadingIncidents.collectAsStateWithLifecycle(false)
        val closeDialog = { showIncidentPicker = false }
        val selectedIncidentId by viewModel.incidentSelector.incidentId.collectAsStateWithLifecycle()
        val setSelected = remember(viewModel) {
            { incident: Incident ->
                viewModel.incidentSelector.selectIncident(incident)
            }
        }
        SelectIncidentDialog(
            rememberKey = viewModel,
            onBackClick = closeDialog,
            incidentsData = incidentsData,
            selectedIncidentId = selectedIncidentId,
            onSelectIncident = setSelected,
            onRefreshIncidentsAsync = viewModel::refreshIncidentsAsync,
            onRefreshIncidents = viewModel::refreshIncidents,
            isLoadingIncidents = isLoadingIncidents,
        )
    }

    if (explainLocationRequest) {
        val permissionExplanation =
            t("info.allow_access_to_location_explanation")
        OpenSettingsDialog(
            t("info.allow_access_to_location"),
            permissionExplanation,
            confirmText = t("info.app_settings"),
            dismissText = t("actions.close"),
        ) {
            explainLocationRequest = false
        }
    }
}

private fun LazyListScope.toggleItem(
    translateKey: String,
    isToggledOn: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    item(
        key = "toggle-$translateKey",
        contentType = "toggle-item",
    ) {
        Row(
            Modifier
                .clickable(
                    onClick = { onToggle(!isToggledOn) },
                )
                .then(listItemModifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                LocalAppTranslator.current(translateKey),
                Modifier.weight(1f),
            )
            Switch(
                checked = isToggledOn,
                onCheckedChange = onToggle,
            )
        }
    }
}

private fun LazyListScope.hotlineSpacerItem() {
    item(contentType = "hotline-spacer") {
        Box(
            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                .fillMaxWidth()
                // TODO Common dimensions
                .height(8.dp),
        )
    }
}

private fun LazyListScope.hotlineItems(
    incidents: List<Incident>,
    expandHotline: Boolean,
    toggleExpandHotline: () -> Unit,
) {
    if (incidents.isNotEmpty()) {
        hotlineSpacerItem()

        item {
            HotlineHeaderView(
                expandHotline,
                toggleExpandHotline,
            )
        }

        if (expandHotline) {
            items(
                incidents,
                key = { "hotline-incident-${it.id}" },
                contentType = { "hotline-incident" },
            ) {
                HotlineIncidentView(it.shortName, it.activePhoneNumbers)
            }
        }

        hotlineSpacerItem()
    }
}

@Composable
private fun MenuTutorial(
    isTutorialDone: Boolean,
    onStartTutorial: () -> Unit,
    onTutorialDone: () -> Unit,
    isNonProduction: Boolean = false,
) {
    val t = LocalAppTranslator.current

    Row(
        listItemModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = t("tutorial.open_tutorial"),
            modifier = Modifier
                .clickable(
                    onClick = onStartTutorial,
                ),
            style = LocalFontStyles.current.header3,
            color = primaryBlueColor,
        )

        if (!isTutorialDone || isNonProduction) {
            Spacer(Modifier.weight(1f))
            Text(
                text = if (!isTutorialDone) t("actions.done") else "Reset",
                modifier = Modifier
                    .clickable(
                        onClick = onTutorialDone,
                    )
                    .listItemPadding(),
                style = LocalFontStyles.current.header4,
                color = primaryBlueColor,
            )
        }
    }
}

@Composable
private fun GettingStartedSection(
    showContent: Boolean,
    hideGettingStartedVideo: () -> Unit,
    gettingStartedUrl: String,
    isNonProduction: Boolean = false,
    toggleGettingStartedSection: (Boolean) -> Unit = { },
) {
    val t = LocalAppTranslator.current

    if (showContent) {
        val uriHandler = LocalUriHandler.current

        Row(
            listItemModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                t("appMenu.training_video"),
                style = LocalFontStyles.current.header2,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = t("actions.hide"),
                modifier = Modifier
                    .clickable(
                        onClick = hideGettingStartedVideo,
                    )
                    .listItemPadding(),
                style = LocalFontStyles.current.header4,
                color = primaryBlueColor,
            )
        }

        Card(
            listItemModifier
                .clickable { uriHandler.openUri(gettingStartedUrl) },
            shape = actionRoundCornerShape,
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        ) {
            Box {
                Image(
                    painterResource(id = R.drawable.getting_starting_video_thumbnail),
                    t("appMenu.getting_started"),
                    Modifier
                        .fillMaxSize()
                        .sizeIn(maxHeight = 128.dp),
                    contentScale = ContentScale.FillWidth,
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape),
                    // TODO Common dimensions
                    color = Color.White.copy(alpha = 0.5f),
                ) {
                    Icon(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(8.dp),
                        imageVector = CrisisCleanupIcons.Play,
                        contentDescription = t("dashboard.play_getting_started_video"),
                    )
                }
            }
            Text(
                t("appMenu.quick_app_intro"),
                // TODO Common dimensions
                Modifier.padding(16.dp),
                style = LocalFontStyles.current.header3,
            )
        }
    } else if (isNonProduction) {
        Text(
            // Not shown on production. Translation is not necessary
            text = t("show getting started section"),
            modifier = Modifier
                .clickable(
                    onClick = { toggleGettingStartedSection(true) },
                )
                .then(listItemModifier),
            style = LocalFontStyles.current.header4,
            color = primaryBlueColor,
        )
    }
}

@Composable
internal fun MenuScreenNonProductionView(
    viewModel: MenuViewModel,
) {
    val databaseText = viewModel.databaseVersionText
    Text(
        modifier = listItemModifier,
        text = databaseText,
    )

    Row(horizontalArrangement = listItemSpacedBy) {
        CrisisCleanupTextButton(
            onClick = { viewModel.clearRefreshToken() },
            text = "Clear refresh token",
        )

        CrisisCleanupTextButton(
            onClick = { viewModel.simulateTokenExpired() },
            text = "Expire token",
        )
    }
}

@Composable
private fun TermsPrivacyView(
    termsOfServiceUrl: String,
    privacyPolicyUrl: String,
) {
    val t = LocalAppTranslator.current
    val uriHandler = LocalUriHandler.current
    Row(
        listItemModifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        CrisisCleanupTextButton(
            Modifier
                .actionHeight()
                .testTag("menuTermsAction"),
            text = t("publicNav.terms"),
        ) {
            uriHandler.openUri(termsOfServiceUrl)
        }
        CrisisCleanupTextButton(
            Modifier
                .actionHeight()
                .testTag("menuPrivacyAction"),
            text = t("nav.privacy"),
        ) {
            uriHandler.openUri(privacyPolicyUrl)
        }
    }
}

@Composable
private fun AppUpdateView() {
    val t = LocalAppTranslator.current
    Row(
        listItemModifier,
        horizontalArrangement = listItemSpacedBy,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var badgeOffsetX by remember { mutableStateOf(0.dp) }
        val localDensity = LocalDensity.current
        BadgedBox(
            badge = {
                Badge(
                    Modifier
                        .size(20.dp)
                        .offset(x = badgeOffsetX),
                    containerColor = primaryOrangeColor,
                ) {
                    // TODO: Match content color in menu badge
                    Icon(
                        imageVector = CrisisCleanupIcons.AppUpdateAvailable,
                        contentDescription = null,
                    )
                }
            },
            Modifier.weight(1f),
        ) {
            Text(
                t("~~A new version of the app is available"),
                Modifier.onGloballyPositioned {
                    badgeOffsetX = with(localDensity) {
                        -it.size.width.div(2).toDp()
                    }
                },
            )
        }

        val context = LocalContext.current
        val playStoreLink =
            "https://play.google.com/store/apps/details?id=${context.packageName}"
        val uriHandler = LocalUriHandler.current
        Text(
            text = t("actions.update"),
            modifier = Modifier
                .clickable(
                    onClick = {
                        uriHandler.openUri(playStoreLink)
                    },
                )
                .listItemPadding(),
            style = LocalFontStyles.current.header4,
            color = primaryBlueColor,
        )
    }
}

@Composable
private fun IncidentCacheView(
    incidentCachePreferences: IncidentWorksitesCachePreferences,
    hasSpeedNotAdaptive: Boolean,
    onOpenIncidentCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalAppTranslator.current

    Column(modifier) {
        if (hasSpeedNotAdaptive) {
            Text(
                t("appMenu.good_internet_use_adaptive"),
                Modifier.listItemBottomPadding(),
            )
        }

        Row(
            horizontalArrangement = listItemSpacedBy,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val syncingPolicy = if (incidentCachePreferences.isPaused) {
                t("appMenu.pause_downloading_cases")
            } else if (incidentCachePreferences.isBoundedNearMe) {
                t("appMenu.download_cases_near_me")
            } else if (incidentCachePreferences.isBoundedByCoordinates) {
                t("appMenu.download_cases_specific_area")
            } else {
                t("appMenu.adaptively_download_cases")
            }
            Text(
                syncingPolicy,
                Modifier.weight(1f),
            )

            Text(
                text = t("actions.change"),
                modifier = Modifier
                    .clickable(
                        onClick = onOpenIncidentCache,
                    )
                    .listItemPadding(),
                style = LocalFontStyles.current.header4,
                color = primaryBlueColor,
            )
        }
    }
}
