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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appcomponent.ui.AppTopBar
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

    val incidentDropdownModifier = Modifier.onGloballyPositioned { coordinates ->
        tutorialViewLookup[TutorialViewId.IncidentSelectDropdown] = coordinates.sizePosition
    }

    val accountToggleModifier = Modifier.onGloballyPositioned { coordinates ->
        tutorialViewLookup[TutorialViewId.AccountToggle] = coordinates.sizePosition
    }

    Column(Modifier.fillMaxWidth()) {
        AppTopBar(
            incidentDropdownModifier = incidentDropdownModifier,
            accountToggleModifier = accountToggleModifier,
            dataProvider = viewModel.appTopBarDataProvider,
            openAuthentication = openAuthentication,
            onOpenIncidents = openIncidentsSelect,
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (!isMenuTutorialDone) {
                MenuTutorial(
                    false,
                    viewModel.menuTutorialDirector::startTutorial,
                    viewModel::setMenuTutorialDone,
                    viewModel.isNotProduction,
                )
            }

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

            CrisisCleanupOutlinedButton(
                modifier = listItemModifier.actionHeight(),
                text = t("list.lists"),
                onClick = openLists,
                enabled = true,
            )

            CrisisCleanupButton(
                modifier = listItemModifier,
                text = t("usersVue.invite_new_user"),
                onClick = openInviteTeammate,
            )

            CrisisCleanupOutlinedButton(
                modifier = listItemModifier.actionHeight(),
                text = t("requestRedeploy.request_redeploy"),
                onClick = openRequestRedeploy,
                enabled = true,
            )

            CrisisCleanupOutlinedButton(
                modifier = listItemModifier.actionHeight(),
                text = t("info.give_app_feedback"),
                onClick = openUserFeedback,
                enabled = true,
            )

            if (isMenuTutorialDone) {
                val unsetMenuTutorialDone =
                    remember(viewModel) { { viewModel.setMenuTutorialDone(false) } }
                MenuTutorial(
                    true,
                    viewModel.menuTutorialDirector::startTutorial,
                    unsetMenuTutorialDone,
                    viewModel.isNotProduction,
                )
            }

            Text(
                viewModel.versionText,
                listItemModifier,
                color = neutralFontColor,
            )

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

            val uriHandler = LocalUriHandler.current

            Spacer(Modifier.weight(1f))

            // TODO Open in WebView?
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

            if (viewModel.isDebuggable) {
                MenuScreenNonProductionView()
            }

            if (viewModel.isNotProduction) {
                CrisisCleanupTextButton(
                    onClick = openSyncLogs,
                    text = "See sync logs",
                )
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
                text = t("actions.done"),
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
