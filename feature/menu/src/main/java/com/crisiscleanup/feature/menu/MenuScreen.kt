package com.crisiscleanup.feature.menu

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
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
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.selectincident.SelectIncidentDialog

@Composable
internal fun MenuRoute(
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
    openUserFeedback: () -> Unit = {},
    openSyncLogs: () -> Unit = {},
) {
    MenuScreen(
        openAuthentication = openAuthentication,
        openInviteTeammate = openInviteTeammate,
        openUserFeedback = openUserFeedback,
        openSyncLogs = openSyncLogs,
    )
}

@Composable
internal fun MenuScreen(
    viewModel: MenuViewModel = hiltViewModel(),
    openAuthentication: () -> Unit = {},
    openInviteTeammate: () -> Unit = {},
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

    Box(Modifier.fillMaxSize()) {
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
                Text(
                    modifier = listItemModifier,
                    text = viewModel.versionText,
                )

                CrisisCleanupButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .listItemPadding(),
                    text = t("usersVue.invite_new_user"),
                    onClick = openInviteTeammate,
                )

                CrisisCleanupOutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .listItemPadding()
                        .actionHeight(),
                    text = t("info.give_app_feedback"),
                    onClick = openUserFeedback,
                    enabled = true,
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

                if (viewModel.isDebuggable) {
                    MenuScreenNonProductionView()
                }

                if (viewModel.isNotProduction) {
                    CrisisCleanupTextButton(
                        onClick = openSyncLogs,
                        text = "See sync logs",
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // TODO Open in WebView?
                val uriHandler = LocalUriHandler.current
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        t("publicNav.terms"),
                        Modifier
                            .listItemPadding()
                            .clickable { uriHandler.openUri("https://crisiscleanup.org/terms") },
                    )
                    Text(
                        t("nav.privacy"),
                        Modifier
                            .listItemPadding()
                            .clickable { uriHandler.openUri("https://crisiscleanup.org/privacy") },
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
