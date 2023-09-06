package com.crisiscleanup.feature.authentication.ui

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
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
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.LinkifyText
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

@Composable
fun RootAuthRoute(
    enableBackHandler: Boolean = false,
    openLoginWithEmail: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
) {
    BackHandler(enableBackHandler) {
        closeAuthentication()
    }

    RootAuthScreen(
        openLoginWithEmail = openLoginWithEmail,
        closeAuthentication = closeAuthentication,
    )
}

@Composable
internal fun RootAuthScreen(
    modifier: Modifier = Modifier,
    viewModel: RootAuthViewModel = hiltViewModel(),
    openLoginWithEmail: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    when (authState) {
        is AuthState.Loading -> {
            Box(Modifier.fillMaxSize()) {
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
    val translator = LocalAppTranslator.current

    Text(
        modifier = fillWidthPadded.testTag("authedProfileAccountInfo"),
        text = translator("info.account_is")
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
        text = translator("actions.logout"),
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
    closeAuthentication: () -> Unit = {},
    hasAuthenticated: Boolean = false,
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val translator = LocalAppTranslator.current
    val uriHandler = LocalUriHandler.current
    val registerHereLink = "https://crisiscleanup.org/register"
    val iNeedHelpCleaningLink = "https://crisiscleanup.org/survivor"
    val closeKeyboard = rememberCloseKeyboard(viewModel)

    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CrisisCleanupLogoRow()

        Text(
            modifier = listItemModifier.testTag("loginHeaderText"),
            text = translator("actions.login", R.string.login),
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
                text = translator("~~Login with Email", R.string.loginWithEmail),
            )
            BusyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loginLoginWithPhoneBtn"),
                onClick = {},
                enabled = false, // !isBusy,
                text = translator("~~Login with Cell Phone", R.string.loginWithPhone),
            )
            CrisisCleanupOutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loginVolunteerWithOrgBtn"),
                onClick = {},
                enabled = false, // !isBusy,
                text = translator(
                    "~~Volunteer with Your Org",
                    R.string.volunteerWithYourOrg,
                ),
            )
            // TODO Open in WebView?
            CrisisCleanupOutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loginNeedHelpCleaningBtn"),
                onClick = {
                    uriHandler.openUri(iNeedHelpCleaningLink)
                },
                enabled = true,
                text = translator(
                    "~~I need help cleaning up",
                    R.string.iNeedHelpCleaningUp,
                ),
            )
        }

        Column(
            modifier = fillWidthPadded,
        ) {
            val linkText = translator("~~Register here", R.string.registerHere)
            val spannableString = SpannableString(linkText).apply {
                setSpan(
                    URLSpan(registerHereLink),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            Text(
                modifier = Modifier.testTag("loginReliefOrgAndGovText"),
                text = translator(
                    "~~Relief organizations and government only.",
                    R.string.reliefOrgAndGovOnly,
                ),
            )
            LinkifyText(
                modifier = Modifier.testTag("loginRegisterHereLink"),
                text = spannableString,
                linkify = { textView ->
                    textView.movementMethod = LinkMovementMethod.getInstance()
                },
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
private fun RootLoginScreenPreview() {
    CrisisCleanupTheme {
        RootAuthScreen()
    }
}
