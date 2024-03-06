package com.crisiscleanup.feature.authentication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupLogoRow
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.OutlinedObfuscatingTextField
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.AuthenticateScreenViewState
import com.crisiscleanup.feature.authentication.AuthenticationViewModel
import com.crisiscleanup.feature.authentication.R
import com.crisiscleanup.feature.authentication.model.AuthenticationState

@Composable
fun LoginWithEmailRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAuthenticated: () -> Unit,
    closeAuthentication: () -> Unit = {},
    openForgotPassword: () -> Unit = {},
    openEmailMagicLink: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val onCloseScreen = remember(viewModel, closeAuthentication) {
        {
            viewModel.onCloseScreen()
            closeAuthentication()
        }
    }

    val isAuthenticateSuccessful by viewModel.isAuthenticateSuccessful.collectAsStateWithLifecycle()
    if (isAuthenticateSuccessful) {
        onAuthenticated()
        onCloseScreen()
    }

    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    when (viewState) {
        is AuthenticateScreenViewState.Loading -> {
            Box {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        is AuthenticateScreenViewState.Ready -> {
            val isKeyboardOpen = rememberIsKeyboardOpen()
            val closeKeyboard = rememberCloseKeyboard(viewModel)

            val readyState = viewState as AuthenticateScreenViewState.Ready
            val authState = readyState.authenticationState
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
                    LoginWithEmailScreen(
                        authState,
                        onBack = onBack,
                        openForgotPassword = openForgotPassword,
                        openEmailMagicLink = openEmailMagicLink,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LoginWithEmailScreen(
    authState: AuthenticationState,
    onBack: () -> Unit = {},
    openForgotPassword: () -> Unit = {},
    openEmailMagicLink: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val translator = LocalAppTranslator.current
    val translateCount by translator.translationCount.collectAsStateWithLifecycle()

    Text(
        modifier = listItemModifier.testTag("loginEmailHeaderText"),
        text = translator("actions.login", R.string.login),
        style = LocalFontStyles.current.header1,
    )

    val authErrorMessage by viewModel.errorMessage
    ConditionalErrorMessage(authErrorMessage)

    val isNotBusy by viewModel.isNotAuthenticating.collectAsStateWithLifecycle()

    val focusEmail = viewModel.loginInputData.emailAddress.isEmpty() ||
        viewModel.isInvalidEmail.value
    val updateEmailInput =
        remember(viewModel) { { s: String -> viewModel.loginInputData.emailAddress = s } }
    val clearErrorVisuals = remember(viewModel) { { viewModel.clearErrorVisuals() } }
    OutlinedClearableTextField(
        modifier = fillWidthPadded.testTag("loginEmailTextField"),
        label = translator("loginForm.email_placeholder", R.string.email),
        value = viewModel.loginInputData.emailAddress,
        onValueChange = updateEmailInput,
        keyboardType = KeyboardType.Email,
        enabled = isNotBusy,
        isError = viewModel.isInvalidEmail.value,
        hasFocus = focusEmail,
        onNext = clearErrorVisuals,
    )

    var isObfuscatingPassword by rememberSaveable { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    val updatePasswordInput =
        remember(viewModel) { { s: String -> viewModel.loginInputData.password = s } }
    val rememberAuth = remember(viewModel) {
        {
            // Allows email input to request focus if necessary
            focusManager.moveFocus(FocusDirection.Next)
            viewModel.authenticateEmailPassword()
        }
    }
    OutlinedObfuscatingTextField(
        modifier = fillWidthPadded.testTag("loginPasswordTextField"),
        label = translator("loginForm.password_placeholder", R.string.password),
        value = viewModel.loginInputData.password,
        onValueChange = updatePasswordInput,
        isObfuscating = isObfuscatingPassword,
        onObfuscate = { isObfuscatingPassword = !isObfuscatingPassword },
        enabled = isNotBusy,
        isError = viewModel.isInvalidPassword.value,
        hasFocus = viewModel.isInvalidPassword.value,
        onEnter = { rememberAuth() },
        imeAction = ImeAction.Done,
    )

    if (translateCount > 0) {
        LinkAction(
            "actions.request_magic_link",
            Modifier
                .testTag("loginRequestMagicLinkAction")
                .actionHeight()
                .listItemPadding(),
            enabled = isNotBusy,
            action = openEmailMagicLink,
        )

        LinkAction(
            "invitationSignup.forgot_password",
            Modifier
                .testTag("loginForgotPasswordAction")
                .actionHeight()
                .listItemPadding(),
            enabled = isNotBusy,
            action = openForgotPassword,
        )
    }

    if (viewModel.isDebug) {
        val rememberDebugAuthenticate = remember(viewModel) {
            {
                viewModel.loginInputData.apply {
                    emailAddress = viewModel.debugEmail
                    password = viewModel.debugPassword
                }
                viewModel.authenticateEmailPassword()
            }
        }
        BusyButton(
            modifier = fillWidthPadded.testTag("emailLoginDebugLoginAction"),
            onClick = rememberDebugAuthenticate,
            enabled = isNotBusy,
            text = "Login debug",
            indicateBusy = !isNotBusy,
        )
    }

    BusyButton(
        modifier = fillWidthPadded.testTag("emailLoginLoginAction"),
        onClick = viewModel::authenticateEmailPassword,
        enabled = isNotBusy,
        text = translator("actions.login", R.string.login),
        indicateBusy = !isNotBusy,
    )

    if (authState.hasAuthenticated) {
        LinkAction(
            "actions.back",
            modifier = Modifier
                .listItemPadding()
                .testTag("emailLoginBackAction"),
            arrangement = Arrangement.Start,
            enabled = isNotBusy,
            action = onBack,
        )
    } else {
        LoginWithDifferentMethod(
            onClick = onBack,
            enabled = isNotBusy,
        )
    }
}
