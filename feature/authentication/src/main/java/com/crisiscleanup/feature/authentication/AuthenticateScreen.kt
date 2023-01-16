package com.crisiscleanup.feature.authentication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.common.R as commonR

@Composable
internal fun AuthenticateRoute(
    modifier: Modifier = Modifier,
) {
    AuthenticateScreen(
        modifier = modifier
    )
}

@OptIn(
    ExperimentalLifecycleComposeApi::class,
)
@Composable
fun AuthenticateScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthenticationViewModel = hiltViewModel(),
    closeAuthentication: () -> Unit = {},
    isDebug: Boolean = false,
) {
    val onCloseScreen = {
        viewModel.loginInputData.password = ""
        closeAuthentication()
    }

    BackHandler {
        onCloseScreen()
    }

    // Collect outside the loading scope so it updates isLoading in the first data mapping
    val authenticationStatus by viewModel.authenticationState.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    if (isLoading) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    } else {
        authenticationStatus.let {
            if (!it.hasAccessToken || it.isTokenExpired) {
                LoginScreen(
                    modifier,
                    onCloseScreen,
                    isDebug = isDebug,
                )

            } else {
                AuthenticatedScreen(
                    modifier,
                    onCloseScreen,
                )
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
                // TODO Make parameter and adjust to screen size
                .sizeIn(maxWidth = 160.dp),
            painter = painterResource(commonR.drawable.crisis_cleanup_logo),
            contentDescription = stringResource(R.string.crisis_cleanup_logo),
        )
    }
}

// TODO Logging in with a different account requires clearing any and all data.
//      Alert that offline data will not upload to different account.

@Composable
private fun conditionalErrorMessage(errorMessage: String) {
    if (errorMessage.isNotEmpty()) {
        Text(
            modifier = fillWidthPadded,
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLifecycleComposeApi::class,
)
@Composable
private fun LoginScreen(
    modifier: Modifier = Modifier,
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
    isDebug: Boolean = false,
) {
    AuthenticateScreenContainer(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        CrisisCleanupLogoRow()

        Text(
            modifier = fillWidthPadded,
            text = stringResource(R.string.login),
            style = MaterialTheme.typography.headlineMedium
        )

        val authErrorMessage by viewModel.errorMessage
        conditionalErrorMessage(authErrorMessage)

        val isNotBusy by viewModel.isNotAuthenticating.collectAsStateWithLifecycle()

        val focusEmail = viewModel.loginInputData.emailAddress.isEmpty() ||
                viewModel.isInvalidEmail.value
        OutlinedClearableTextField(
            modifier = fillWidthPadded,
            labelResId = R.string.email,
            value = viewModel.loginInputData.emailAddress,
            onValueChange = { viewModel.loginInputData.emailAddress = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = isNotBusy,
            isError = viewModel.isInvalidEmail.value,
            hasFocus = focusEmail,
        )

        OutlinedClearableTextField(
            modifier = fillWidthPadded,
            labelResId = R.string.password,
            value = viewModel.loginInputData.password,
            onValueChange = { viewModel.loginInputData.password = it },
            obfuscateValue = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = isNotBusy,
            isError = viewModel.isInvalidPassword.value,
            hasFocus = viewModel.isInvalidPassword.value,
        )

        if (isDebug) {
            Button(
                modifier = fillWidthPadded,
                onClick = {
                    viewModel.loginInputData.apply {
                        emailAddress = "demo@crisiscleanup.org"
                        password = "demodemo1"
                    }
                    viewModel.authenticateEmailPassword()
                },
                enabled = isNotBusy,
            ) {
                Text("Login debug")
            }
        }

        // TODO Login button with loading
        Button(
            modifier = fillWidthPadded,
            onClick = { viewModel.authenticateEmailPassword() },
            enabled = isNotBusy,
        ) {
            Text(stringResource(id = R.string.login))
        }

        val authState = viewModel.authenticationState.collectAsStateWithLifecycle()
        if (authState.value.hasAccessToken) {
            Button(
                modifier = fillWidthPadded,
                onClick = closeAuthentication,
                enabled = isNotBusy,
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@OptIn(
    ExperimentalLifecycleComposeApi::class,
)
@Composable
private fun AuthenticatedScreen(
    modifier: Modifier = Modifier,
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    AuthenticateScreenContainer(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        CrisisCleanupLogoRow()

        val authState by viewModel.authenticationState.collectAsStateWithLifecycle()

        Text(
            modifier = fillWidthPadded,
            text = stringResource(
                R.string.account_is,
                authState.accountData.displayName,
                authState.accountData.emailAddress
            ),
        )

        val authErrorMessage by viewModel.errorMessage
        conditionalErrorMessage(authErrorMessage)

        val isNotBusy by viewModel.isNotAuthenticating.collectAsStateWithLifecycle()

        // TODO Logout button with loading
        Button(
            modifier = fillWidthPadded,
            onClick = { viewModel.logout() },
            enabled = isNotBusy,
        ) {
            Text(stringResource(id = R.string.logout))
        }

        Button(
            modifier = fillWidthPadded,
            onClick = closeAuthentication,
        ) {
            Text(stringResource(R.string.dismiss))
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}

@Preview
@Composable
fun AuthenticatedScreenPreview() {
    AuthenticatedScreen()
}