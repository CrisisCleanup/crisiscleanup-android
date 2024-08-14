package com.crisiscleanup.feature.menu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appcomponent.ui.AppTopBar
import com.crisiscleanup.core.common.TutorialStep
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.component.actionRoundCornerShape
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.cardContainerColor
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.neutralFontColor
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.TutorialViewId
import com.crisiscleanup.core.selectincident.SelectIncidentDialog
import com.crisiscleanup.core.ui.sizePosition
import com.crisiscleanup.feature.menu.MenuViewModel
import com.crisiscleanup.feature.menu.R

@Composable
internal fun MenuRoute(
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openRequestRedeploy: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openLists: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    MenuScreen(
        openAuthentication = openAuthentication,
        openInviteTeammate = openInviteTeammate,
        openRequestRedeploy = openRequestRedeploy,
        openUserFeedback = openUserFeedback,
        openLists = openLists,
        openSyncLogs = openSyncLogs,
    )
}

@Composable
private fun MenuScreen(
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openRequestRedeploy: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openLists: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    val incidentsData by viewModel.incidentsData.collectAsStateWithLifecycle()

    var showIncidentPicker by remember { mutableStateOf(false) }
    val openIncidentsSelect = remember(viewModel) {
        { showIncidentPicker = true }
    }

    val isSharingAnalytics by viewModel.isSharingAnalytics.collectAsStateWithLifecycle(false)
    val shareAnalytics = remember(viewModel) {
        { b: Boolean ->
            viewModel.shareAnalytics(b)
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

    Column {
        AppTopBar(
            incidentDropdownModifier = incidentDropdownModifier,
            accountToggleModifier = accountToggleModifier,
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
                var listItemIndex = itemIndex
                if (!isMenuTutorialDone) {
                    listItemIndex += 1
                }
                if (isGettingStartedVisible) {
                    listItemIndex += 1
                }
                return listItemIndex
            }
            when (tutorialStep) {
                TutorialStep.MenuStart,
                TutorialStep.InviteTeammates,
                -> {
                    val listItemIndex = getListItemIndex(2)
                    if (firstVisibleItemIndex > listItemIndex - 2 ||
                        lastVisibleItemIndex < listItemIndex + 2
                    ) {
                        lazyListState.scrollToItem(listItemIndex, focusItemScrollOffset)
                    }
                }

                TutorialStep.ProvideAppFeedback -> {
                    val listItemIndex = getListItemIndex(4)
                    if (firstVisibleItemIndex > listItemIndex - 2 ||
                        lastVisibleItemIndex < listItemIndex + 2
                    ) {
                        lazyListState.scrollToItem(listItemIndex, focusItemScrollOffset)
                    }
                }

                else -> {}
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            state = lazyListState,
        ) {
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
                CrisisCleanupButton(
                    modifier = inviteTeammateModifier
                        .fillMaxWidth()
                        .listItemPadding(),
                    text = t("usersVue.invite_new_user"),
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

            item {
                Row(
                    Modifier
                        .clickable(
                            onClick = { shareAnalytics(!isSharingAnalytics) },
                        )
                        .then(listItemModifier),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        t("actions.share_analytics"),
                        Modifier.weight(1f),
                    )
                    Switch(
                        checked = isSharingAnalytics,
                        onCheckedChange = shareAnalytics,
                    )
                }
            }

            // TODO Open in WebView?
            item {
                val uriHandler = LocalUriHandler.current
                Row(
                    listItemModifier,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CrisisCleanupTextButton(
                        Modifier.actionHeight(),
                        text = t("publicNav.terms"),
                    ) {
                        uriHandler.openUri(viewModel.termsOfServiceUrl)
                    }
                    CrisisCleanupTextButton(
                        Modifier.actionHeight(),
                        text = t("nav.privacy"),
                    ) {
                        uriHandler.openUri(viewModel.privacyPolicyUrl)
                    }
                }
            }

            if (viewModel.isDebuggable) {
                item {
                    MenuScreenNonProductionView()
                }
            }

            if (viewModel.isNotProduction) {
                item {
                    CrisisCleanupTextButton(
                        onClick = openSyncLogs,
                        text = "See sync logs",
                    )
                }
            }
        }
    }

    if (showIncidentPicker) {
        val closeDialog = { showIncidentPicker = false }
        val selectedIncidentId by viewModel.incidentSelector.incidentId.collectAsStateWithLifecycle()
        val setSelected = remember(viewModel) {
            { incident: Incident ->
                viewModel.loadSelectIncidents.selectIncident(incident)
            }
        }
        SelectIncidentDialog(
            rememberKey = viewModel,
            onBackClick = closeDialog,
            incidentsData = incidentsData,
            selectedIncidentId = selectedIncidentId,
            onSelectIncident = setSelected,
            onRefreshIncidents = viewModel::refreshIncidentsAsync,
        )
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
            text = t("~~Open tutorial"),
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
    viewModel: MenuViewModel = hiltViewModel(),
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

    CrisisCleanupTextButton(
        onClick = { viewModel.syncWorksitesFull() },
        text = "Sync full",
    )
}
