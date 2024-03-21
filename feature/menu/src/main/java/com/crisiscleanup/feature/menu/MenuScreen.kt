package com.crisiscleanup.feature.menu

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.ui.IncidentDropdownSelect
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.TopAppBarDefault
import com.crisiscleanup.core.designsystem.component.TruncatedAppBarText
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
import com.crisiscleanup.core.selectincident.SelectIncidentDialog

@Composable
internal fun MenuRoute(
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openRequestRedeploy: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    MenuScreen(
        openAuthentication = openAuthentication,
        openInviteTeammate = openInviteTeammate,
        openRequestRedeploy = openRequestRedeploy,
        openUserFeedback = openUserFeedback,
        openSyncLogs = openSyncLogs,
    )
}

@Composable
internal fun MenuScreen(
    viewModel: MenuViewModel = hiltViewModel(),
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openRequestRedeploy: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    val t = LocalAppTranslator.current

    val screenTitle by viewModel.screenTitle.collectAsStateWithLifecycle()
    val isHeaderLoading by viewModel.showHeaderLoading.collectAsState(false)

    val incidentsData by viewModel.incidentsData.collectAsStateWithLifecycle()
    val disasterIconResId by viewModel.disasterIconResId.collectAsStateWithLifecycle()

    var showIncidentPicker by remember { mutableStateOf(false) }
    val openIncidentsSelect = remember(viewModel) {
        { showIncidentPicker = true }
    }

    val isAccountExpired by viewModel.isAccountExpired.collectAsStateWithLifecycle()
    val profilePictureUri by viewModel.profilePictureUri.collectAsStateWithLifecycle()

    val isSharingAnalytics by viewModel.isSharingAnalytics.collectAsStateWithLifecycle(false)
    val shareAnalytics = remember(viewModel) {
        { b: Boolean ->
            viewModel.shareAnalytics(b)
        }
    }

    val menuItemVisibility by viewModel.menuItemVisibility.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth()) {
        TopBar(
            modifier = Modifier,
            title = screenTitle,
            isAppHeaderLoading = isHeaderLoading,
            profilePictureUri = profilePictureUri,
            isAccountExpired = isAccountExpired,
            openAuthentication = openAuthentication,
            disasterIconResId = disasterIconResId,
            onOpenIncidents = openIncidentsSelect,
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val hideGettingStartedVideo = remember(viewModel) {
                { viewModel.showGettingStartedVideo(false) }
            }
            GettingStartedSection(
                menuItemVisibility.showGettingStartedVideo,
                hideGettingStartedVideo,
                viewModel.gettingStartedVideoUrl,
                viewModel.isNotProduction,
                toggleGettingStartedSection = viewModel::showGettingStartedVideo,
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
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    title: String = "",
    isAppHeaderLoading: Boolean = false,
    profilePictureUri: String = "",
    isAccountExpired: Boolean = false,
    openAuthentication: () -> Unit = {},
    @DrawableRes disasterIconResId: Int = 0,
    onOpenIncidents: (() -> Unit)? = null,
) {
    val t = LocalAppTranslator.current
    val actionText = t("actions.account")
    TopAppBarDefault(
        modifier = modifier,
        title = title,
        profilePictureUri = profilePictureUri,
        actionIcon = CrisisCleanupIcons.Account,
        actionText = actionText,
        isActionAttention = isAccountExpired,
        onActionClick = openAuthentication,
        onNavigationClick = null,
        titleContent = @Composable {
            // TODO Match height of visible part of app bar (not the entire app bar)
            if (onOpenIncidents == null) {
                TruncatedAppBarText(title = title)
            } else {
                IncidentDropdownSelect(
                    modifier = Modifier.testTag("appIncidentSelector"),
                    onOpenIncidents,
                    disasterIconResId,
                    title = title,
                    contentDescription = t("nav.change_incident"),
                    isLoading = isAppHeaderLoading,
                )
            }
        },
    )
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
            Image(
                painterResource(id = R.drawable.getting_starting_video_thumbnail),
                t("appMenu.getting_started"),
                Modifier
                    .fillMaxSize()
                    .sizeIn(maxHeight = 128.dp),
                contentScale = ContentScale.FillWidth,
            )
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
