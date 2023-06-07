package com.crisiscleanup.feature.authentication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
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
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
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
    // TODO Write test(s)
    val onCloseScreen = remember(viewModel, closeAuthentication) {
        {
            viewModel.onCloseScreen()
            closeAuthentication()
        }
    }

    // TODO Write test
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
            readyState.authenticationState.let {
                if (it.isTokenInvalid) {
                    LoginScreen(
                        it,
                        modifier,
                        onCloseScreen,
                        isDebug = isDebug,
                    )

                } else {
                    AuthenticatedScreen(
                        it,
                        modifier,
                        onCloseScreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticateScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        Column(Modifier.fillMaxWidth()) {
            content()
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun LoginScreen(
    authState: AuthenticationState,
    modifier: Modifier = Modifier,
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
    isDebug: Boolean = false,
) {
    val translator = LocalAppTranslator.current.translator
    AuthenticateScreenContainer(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        CrisisCleanupLogoRow()

        val onInputFocus = remember(viewModel) {
            { focusState: FocusState ->
                if (focusState.hasFocus) {
                    viewModel.onInputFocus()
                }
            }
        }

        Text(
            modifier = fillWidthPadded,
            text = translator("actions.login", R.string.login),
            style = MaterialTheme.typography.headlineMedium
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
            // TODO Listening onFocusChanged forces an input to recompose on any input change.
            //      Research if possible to decouple focus change when not in focus.
            modifier = fillWidthPadded.onFocusChanged(onInputFocus),
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
            modifier = fillWidthPadded.onFocusChanged(onInputFocus),
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
                modifier = fillWidthPadded,
                onClick = rememberDebugAuthenticate,
                enabled = isNotBusy,
                text = "Login debug",
                indicateBusy = !isNotBusy,
            )
        }

        BusyButton(
            modifier = fillWidthPadded,
            onClick = viewModel::authenticateEmailPassword,
            enabled = isNotBusy,
            text = translator("actions.login", R.string.login),
            indicateBusy = !isNotBusy,
        )

        if (authState.hasAccessToken) {
            CrisisCleanupButton(
                modifier = fillWidthPadded,
                onClick = closeAuthentication,
                enabled = isNotBusy,
                text = translator("actions.cancel", R.string.cancel),
            )
        }
    }
}

@Composable
private fun AuthenticatedScreen(
    authState: AuthenticationState,
    modifier: Modifier = Modifier,
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val translator = LocalAppTranslator.current.translator
    AuthenticateScreenContainer(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        CrisisCleanupLogoRow()

        Text(
            modifier = fillWidthPadded,
            text = stringResource(
                R.string.account_is,
                authState.accountData.fullName,
                authState.accountData.emailAddress
            ),
        )

        val authErrorMessage by viewModel.errorMessage
        ConditionalErrorMessage(authErrorMessage)

        val isNotBusy by viewModel.isNotAuthenticating.collectAsStateWithLifecycle()

        SaveCredentialsPrompt(
            viewModel,
            closeAuthentication,
            isNotBusy,
        )

        BusyButton(
            modifier = fillWidthPadded,
            onClick = viewModel::logout,
            enabled = isNotBusy,
            text = translator("actions.logout"),
            indicateBusy = !isNotBusy,
        )

        CrisisCleanupButton(
            modifier = fillWidthPadded,
            onClick = closeAuthentication,
            enabled = isNotBusy,
            textResId = R.string.dismiss,
        )
    }
}

@Composable
private fun SaveCredentialsPrompt(
    viewModel: AuthenticationViewModel = hiltViewModel(),
    closeAuthentication: () -> Unit = {},
    isNotBusy: Boolean,
) {
    val showSaveCredentials by viewModel.showSaveCredentialsAction
    if (showSaveCredentials) {
        Text(
            modifier = fillWidthPadded,
            text = stringResource(R.string.prompt_save_password),
        )

        BusyButton(
            modifier = fillWidthPadded,
            onClick = {
                viewModel.saveCredentials()
                closeAuthentication()
            },
            enabled = isNotBusy,
            text = LocalAppTranslator.current.translator("actions.save"),
            indicateBusy = !isNotBusy,
        )

        val showDisableSave by viewModel.showDisableSaveCredentials.collectAsStateWithLifecycle()
        if (showDisableSave) {
            var disableSave by remember { mutableStateOf(false) }
            val setDisableSave = remember(viewModel) {
                { disable: Boolean ->
                    disableSave = disable
                    viewModel.setDisableSaveCredentials(disable)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { setDisableSave(!disableSave) },
                verticalAlignment = Alignment.CenterVertically,

                ) {
                Checkbox(
                    checked = disableSave,
                    onCheckedChange = setDisableSave
                )

                Text(stringResource(R.string.do_not_ask_save_credentials))
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

@DayNightPreviews
@Composable
fun LogoRowPreview() {
    CrisisCleanupLogoRow()
}
