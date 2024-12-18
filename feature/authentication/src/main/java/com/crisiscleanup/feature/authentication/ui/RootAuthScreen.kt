package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupLogoRow
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.HotlineHeaderView
import com.crisiscleanup.core.designsystem.component.HotlineIncidentView
import com.crisiscleanup.core.designsystem.component.LinkifyText
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.DayNightPreviews
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.AuthState
import com.crisiscleanup.feature.authentication.AuthenticationViewModel
import com.crisiscleanup.feature.authentication.R
import com.crisiscleanup.feature.authentication.RootAuthViewModel

internal const val ORG_REGISTER_URL = "https://crisiscleanup.org/register"

@Composable
fun RootAuthRoute(
    enableBackHandler: Boolean = false,
    openLoginWithEmail: () -> Unit = {},
    openLoginWithPhone: () -> Unit = {},
    openVolunteerOrg: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
) {
    BackHandler(enableBackHandler) {
        closeAuthentication()
    }

    RootAuthScreen(
        openLoginWithEmail = openLoginWithEmail,
        openLoginWithPhone = openLoginWithPhone,
        openVolunteerOrg = openVolunteerOrg,
        closeAuthentication = closeAuthentication,
    )
}

@Composable
internal fun RootAuthScreen(
    openLoginWithEmail: () -> Unit = {},
    openLoginWithPhone: () -> Unit = {},
    openVolunteerOrg: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
    viewModel: RootAuthViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    when (authState) {
        is AuthState.Loading -> {
            Box {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        is AuthState.Authenticated -> {
            val isKeyboardOpen = rememberIsKeyboardOpen()
            val closeKeyboard = rememberCloseKeyboard(viewModel)
            val accountData = (authState as AuthState.Authenticated).accountData
            val contentModifier = Modifier
                .fillMaxSize()
                .scrollFlingListener(closeKeyboard)
                .verticalScroll(rememberScrollState())
            if (LocalDimensions.current.isListDetailWidth) {
                Row {
                    Column(Modifier.weight(1f)) {
                        AuthenticatedScreen(
                            accountData,
                            closeAuthentication,
                        )
                    }
                    CrisisCleanupLogoRow(Modifier.weight(1f))
                }
            } else {
                Column(contentModifier) {
                    AnimatedVisibility(visible = !isKeyboardOpen) {
                        CrisisCleanupLogoRow()
                    }
                    AuthenticatedScreen(
                        accountData,
                        closeAuthentication,
                    )
                }
            }
        }

        is AuthState.NotAuthenticated -> {
            val hasAuthenticated = (authState as AuthState.NotAuthenticated).hasAuthenticated
            val hotlineIncidents by viewModel.hotlineIncidents.collectAsStateWithLifecycle()
            NotAuthenticatedScreen(
                openLoginWithEmail = openLoginWithEmail,
                openLoginWithPhone = openLoginWithPhone,
                openVolunteerOrg = openVolunteerOrg,
                closeAuthentication = closeAuthentication,
                hasAuthenticated = hasAuthenticated,
                hotlineIncidents = hotlineIncidents,
            )
        }
    }
}

@Composable
private fun AuthenticatedScreen(
    accountData: AccountData,
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    Text(
        modifier = fillWidthPadded.testTag("authedAccountInfoText"),
        text = t("info.account_is")
            .replace("{full_name}", accountData.fullName)
            .replace("{email_address}", accountData.emailAddress),
    )

    val authErrorMessage by viewModel.errorMessage
    ConditionalErrorMessage(authErrorMessage, "authenticated")

    val isNotBusy by viewModel.isNotAuthenticating.collectAsStateWithLifecycle()

    BusyButton(
        modifier = fillWidthPadded.testTag("authedLogoutAction"),
        onClick = viewModel::logout,
        enabled = isNotBusy,
        text = t("actions.logout"),
        indicateBusy = !isNotBusy,
    )

    LinkAction(
        "actions.back",
        modifier = Modifier
            .listItemPadding()
            .testTag("authedCloseScreenAction"),
        arrangement = Arrangement.Start,
        enabled = isNotBusy,
        action = closeAuthentication,
    )
}

@Composable
private fun NotAuthenticatedScreen(
    openLoginWithEmail: () -> Unit = {},
    openLoginWithPhone: () -> Unit = {},
    openVolunteerOrg: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
    hasAuthenticated: Boolean = false,
    hotlineIncidents: List<Incident> = emptyList(),
) {
    val t = LocalAppTranslator.current
    val translationCount by t.translationCount.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val iNeedHelpCleaningLink = "https://crisiscleanup.org/survivor"
    val closeKeyboard = rememberCloseKeyboard(openLoginWithEmail)

    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CrisisCleanupLogoRow()

        HotlineIncidentsView(hotlineIncidents)

        Text(
            modifier = listItemModifier.testTag("rootAuthLoginText"),
            text = t("actions.login", R.string.login),
            style = LocalFontStyles.current.header1,
        )

        Column(
            modifier = fillWidthPadded,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BusyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loginWithEmailAction"),
                onClick = openLoginWithEmail,
                text = t("loginForm.login_with_email", R.string.loginWithEmail),
            )

            BusyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loginWithPhoneAction"),
                onClick = openLoginWithPhone,
                text = t("loginForm.login_with_cell", R.string.loginWithPhone),
            )

            CrisisCleanupOutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .actionHeight()
                    .testTag("rootAuthVolunteerWithOrgAction"),
                onClick = openVolunteerOrg,
                enabled = !hasAuthenticated,
                text = t(
                    "actions.request_access",
                    R.string.volunteerWithYourOrg,
                ),
            )

            // TODO Open in WebView?
            CrisisCleanupOutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .actionHeight()
                    .testTag("rootAuthNeedHelpAction"),
                onClick = {
                    uriHandler.openUri(iNeedHelpCleaningLink)
                },
                enabled = true,
                text = t(
                    "loginForm.need_help_cleaning_up",
                    R.string.iNeedHelpCleaningUp,
                ),
            )
        }

        Column(fillWidthPadded) {
            Text(
                modifier = Modifier.testTag("rootAuthReliefOrgAndGovText"),
                text = t(
                    "publicNav.relief_orgs_only",
                    R.string.reliefOrgAndGovOnly,
                ),
            )
            val registerText = remember(translationCount) {
                t("actions.register")
            }
            LinkifyText(
                modifier = Modifier.testTag("rootAuthRegisterAction"),
                linkText = registerText,
                link = ORG_REGISTER_URL,
            )
        }

        if (hasAuthenticated) {
            LinkAction(
                "actions.back",
                modifier = Modifier
                    .listItemPadding()
                    .testTag("rootAuthBackBtn"),
                arrangement = Arrangement.Start,
                enabled = true,
                action = closeAuthentication,
            )
        }
    }
}

@Composable
private fun HotlineIncidentsView(
    incidents: List<Incident>,
) {
    if (incidents.isNotEmpty()) {
        var expandHotline by remember { mutableStateOf(true) }
        val toggleExpandHotline = { expandHotline = !expandHotline }

        // TODO Common dimensions
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 8.dp),
        ) {
            HotlineHeaderView(
                expandHotline,
                toggleExpandHotline,
            )
            if (expandHotline) {
                for (incident in incidents) {
                    HotlineIncidentView(
                        incident.shortName,
                        incident.activePhoneNumbers,
                        linkifyNumbers = true,
                    )
                }
            }
        }
    }
}

@DayNightPreviews
@Composable
private fun NotAuthenticatedScreenPreview() {
    CrisisCleanupTheme {
        NotAuthenticatedScreen()
    }
}
