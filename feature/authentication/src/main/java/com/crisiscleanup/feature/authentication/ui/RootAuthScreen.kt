package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.crisiscleanup.core.designsystem.component.LinkifyText
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.DayNightPreviews
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.AuthState
import com.crisiscleanup.feature.authentication.AuthenticationViewModel
import com.crisiscleanup.feature.authentication.R
import com.crisiscleanup.feature.authentication.RootAuthViewModel

internal const val orgRegisterLink = "https://crisiscleanup.org/register"

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
    modifier: Modifier = Modifier,
    viewModel: RootAuthViewModel = hiltViewModel(),
    openLoginWithEmail: () -> Unit = {},
    openLoginWithPhone: () -> Unit = {},
    openVolunteerOrg: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
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
            Box(modifier) {
                Column(
                    Modifier
                        .scrollFlingListener(closeKeyboard)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
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
            NotAuthenticatedScreen(
                openLoginWithEmail = openLoginWithEmail,
                openLoginWithPhone = openLoginWithPhone,
                openVolunteerOrg = openVolunteerOrg,
                closeAuthentication = closeAuthentication,
                hasAuthenticated = hasAuthenticated,
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
        modifier = fillWidthPadded.testTag("authedProfileAccountInfo"),
        text = t("info.account_is")
            .replace("{full_name}", accountData.fullName)
            .replace("{email_address}", accountData.emailAddress),
    )

    val authErrorMessage by viewModel.errorMessage
    ConditionalErrorMessage(authErrorMessage)

    val isNotBusy by viewModel.isNotAuthenticating.collectAsStateWithLifecycle()

    BusyButton(
        modifier = fillWidthPadded.testTag("authedProfileLogoutBtn"),
        onClick = viewModel::logout,
        enabled = isNotBusy,
        text = t("actions.logout"),
        indicateBusy = !isNotBusy,
    )

    LinkAction(
        "actions.back",
        modifier = Modifier
            .listItemPadding()
            .testTag("authedProfileDismissBtn"),
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
) {
    val t = LocalAppTranslator.current
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

        Text(
            modifier = listItemModifier.testTag("loginHeaderText"),
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
                    .testTag("loginLoginWithEmailBtn"),
                onClick = openLoginWithEmail,
                text = t("loginForm.login_with_email", R.string.loginWithEmail),
            )

            BusyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loginLoginWithPhoneBtn"),
                onClick = openLoginWithPhone,
                text = t("loginForm.login_with_cell", R.string.loginWithPhone),
            )

            CrisisCleanupOutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .actionHeight()
                    .testTag("loginVolunteerWithOrgBtn"),
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
                    .testTag("loginNeedHelpCleaningBtn"),
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
                modifier = Modifier.testTag("loginReliefOrgAndGovText"),
                text = t(
                    "publicNav.relief_orgs_only",
                    R.string.reliefOrgAndGovOnly,
                ),
            )
            LinkifyText(
                linkText = t("actions.register"),
                link = orgRegisterLink,
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

@DayNightPreviews
@Composable
private fun NotAuthenticatedScreenPreview() {
    CrisisCleanupTheme {
        NotAuthenticatedScreen()
    }
}
