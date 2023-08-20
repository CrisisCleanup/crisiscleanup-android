package com.crisiscleanup.feature.authentication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.OutlinedObfuscatingTextField
import com.crisiscleanup.core.designsystem.theme.DayNightPreviews
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.feature.authentication.model.AuthenticationState
import com.crisiscleanup.core.common.R as commonR

@Composable
fun AuthenticateScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthenticationViewModel = hiltViewModel(),
    closeAuthentication: () -> Unit = {},
    enableBackHandler: Boolean = false,
    isDebug: Boolean = false,
) {
    val onCloseScreen = remember(viewModel, closeAuthentication) {
        {
            viewModel.onCloseScreen()
            closeAuthentication()
        }
    }

    val isAuthenticateSuccessful by viewModel.isAuthenticateSuccessful.collectAsStateWithLifecycle()
    if (isAuthenticateSuccessful) {
        onCloseScreen()
    }

    BackHandler(enableBackHandler) {
        onCloseScreen()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState) {
        AuthenticateScreenUiState.Loading -> {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        is AuthenticateScreenUiState.Ready -> {
            val readyState = uiState as AuthenticateScreenUiState.Ready
            val authState = readyState.authenticationState
            Box(modifier) {
                // TODO Scroll when content is longer than screen height with keyboard open
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    CrisisCleanupLogoRow()

                    if (authState.isAccountValid) {
                        AuthenticatedScreen(
                            authState,
                            onCloseScreen,
                        )
                    } else {
                        LoginScreen(
                            authState,
                            onCloseScreen,
                            isDebug = isDebug,
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CrisisCleanupLogoRow() {
    Row(
        modifier = fillWidthPadded,
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(
            modifier = Modifier
                .testTag("ccuLogo")
                // TODO Adjust image size to screen size
                .sizeIn(maxWidth = 160.dp),
            painter = painterResource(commonR.drawable.crisis_cleanup_logo),
            contentDescription = stringResource(com.crisiscleanup.core.common.R.string.crisis_cleanup),
        )
    }
}

@Composable
private fun ConditionalErrorMessage(errorMessage: String) {
    if (errorMessage.isNotEmpty()) {
        Text(
            modifier = fillWidthPadded,
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun LoginScreen(
    authState: AuthenticationState,
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
    isDebug: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    Text(
        modifier = listItemModifier.testTag("loginHeaderText"),
        text = translator("actions.login", R.string.login),
        style = LocalFontStyles.current.header2,
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

    if (isDebug) {
        val rememberDebugAuthenticate = remember(viewModel) {
            {
                viewModel.loginInputData.apply {
                    emailAddress = BuildConfig.DEBUG_EMAIL_ADDRESS
                    password = BuildConfig.DEBUG_ACCOUNT_PASSWORD
                }
                viewModel.authenticateEmailPassword()
            }
        }
        BusyButton(
            modifier = fillWidthPadded.testTag("loginLoginDebugBtn"),
            onClick = rememberDebugAuthenticate,
            enabled = isNotBusy,
            text = "Login debug",
            indicateBusy = !isNotBusy,
        )
    }

    BusyButton(
        modifier = fillWidthPadded.testTag("loginLoginBtn"),
        onClick = viewModel::authenticateEmailPassword,
        enabled = isNotBusy,
        text = translator("actions.login", R.string.login),
        indicateBusy = !isNotBusy,
    )

    if (authState.hasAuthenticated) {
        CrisisCleanupButton(
            modifier = fillWidthPadded.testTag("loginCancelBtn"),
            onClick = closeAuthentication,
            enabled = isNotBusy,
            text = translator("actions.cancel", R.string.cancel),
        )
    }
}

@Composable
private fun AuthenticatedScreen(
    authState: AuthenticationState,
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val translator = LocalAppTranslator.current

    Text(
        modifier = fillWidthPadded.testTag("authedProfileAccountInfo"),
        text = translator("info.account_is")
            .replace("{full_name}", authState.accountData.fullName)
            .replace("{email_address}", authState.accountData.emailAddress),
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

    CrisisCleanupButton(
        modifier = fillWidthPadded.testTag("authedProfileDismissBtn"),
        onClick = closeAuthentication,
        enabled = isNotBusy,
        text = translator("actions.dismiss"),
    )
}

@DayNightPreviews
@Composable
fun LogoRowPreview() {
    CrisisCleanupLogoRow()
}
