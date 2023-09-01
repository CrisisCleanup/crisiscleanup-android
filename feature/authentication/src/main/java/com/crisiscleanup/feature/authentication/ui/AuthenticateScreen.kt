package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
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
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.OutlinedObfuscatingTextField
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.DayNightPreviews
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.actionLinkColor
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.AuthenticateScreenUiState
import com.crisiscleanup.feature.authentication.AuthenticationViewModel
import com.crisiscleanup.feature.authentication.BuildConfig
import com.crisiscleanup.feature.authentication.R
import com.crisiscleanup.feature.authentication.model.AuthenticationState
import com.crisiscleanup.core.common.R as commonR

@Composable
fun AuthRoute(
    enableBackHandler: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    openForgotPassword: () -> Unit = {},
    openEmailMagicLink: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    // TODO Push route rather than toggling state
    val showResetPassword by viewModel.showResetPassword.collectAsStateWithLifecycle(false)
    if (showResetPassword) {
        PasswordRecoverRoute(
            onBack = viewModel::clearResetPassword,
            showResetPassword = true,
        )
    } else {
        AuthenticateScreen(
            enableBackHandler = enableBackHandler,
            modifier = modifier,
            onBack = onBack,
            openForgotPassword = openForgotPassword,
            openEmailMagicLink = openEmailMagicLink,
            closeAuthentication = closeAuthentication,
        )
    }
}

@Composable
private fun AuthenticateScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthenticationViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    openForgotPassword: () -> Unit = {},
    openEmailMagicLink: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
    enableBackHandler: Boolean = false,
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
        is AuthenticateScreenUiState.Loading -> {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        is AuthenticateScreenUiState.Ready -> {
            val isKeyboardOpen = rememberIsKeyboardOpen()
            val closeKeyboard = rememberCloseKeyboard(viewModel)

            val readyState = uiState as AuthenticateScreenUiState.Ready
            val authState = readyState.authenticationState
            Box(modifier) {
                // TODO Scroll when content is longer than screen height with keyboard open
                Column(
                    Modifier
                        .scrollFlingListener(closeKeyboard)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    AnimatedVisibility(visible = !isKeyboardOpen) {
                        CrisisCleanupLogoRow()
                    }

                    if (authState.isAccountValid) {
                        AuthenticatedScreen(
                            authState,
                            onCloseScreen,
                        )
                    } else {
                        LoginScreen(
                            authState,
                            onBack = onBack,
                            openForgotPassword = openForgotPassword,
                            openEmailMagicLink = openEmailMagicLink,
                            closeAuthentication = onCloseScreen,
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun CrisisCleanupLogoRow() {
    // TODO Adjust to other screen sizes as necessary
    Box(Modifier.padding(top = 16.dp, start = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Image(
                painterResource(R.drawable.worker_wheelbarrow_world_background),
                modifier = Modifier
                    .testTag("ccuBackground")
                    .padding(top = 32.dp)
                    .size(width = 480.dp, height = 240.dp)
                    .offset(x = 64.dp),
                contentScale = ContentScale.FillHeight,
                contentDescription = null,
            )
        }
        Row(
            modifier = fillWidthPadded,
            horizontalArrangement = Arrangement.Start,
        ) {
            Image(
                modifier = Modifier
                    .testTag("ccuLogo")
                    .sizeIn(maxWidth = 160.dp),
                painter = painterResource(commonR.drawable.crisis_cleanup_logo),
                contentDescription = stringResource(com.crisiscleanup.core.common.R.string.crisis_cleanup),
            )
        }
    }
}

@Composable
fun LoginWithDifferentMethod(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val translator = LocalAppTranslator.current
    TextButton(
        modifier = modifier.padding(horizontal = 16.dp),
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Login using different method",
                tint = actionLinkColor,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(24.dp),
            )
            Text(
                text = translator(
                    "~~Login using different method",
                    R.string.loginUsingDifferentMethod,
                ),
                color = actionLinkColor,
                style = LocalFontStyles.current.header3,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
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
private fun LinkAction(
    textTranslateKey: String,
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.End,
    enabled: Boolean = false,
    action: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
    ) {
        Text(
            text = translator(textTranslateKey),
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    onClick = action,
                )
                .then(modifier),
            style = LocalFontStyles.current.header4,
            color = primaryBlueColor,
        )
    }
}

@Composable
private fun LoginScreen(
    authState: AuthenticationState,
    onBack: () -> Unit = {},
    openForgotPassword: () -> Unit = {},
    openEmailMagicLink: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val translator = LocalAppTranslator.current
    val translateCount by translator.translationCount.collectAsStateWithLifecycle()

    Text(
        modifier = listItemModifier.testTag("loginHeaderText"),
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
//        LinkAction(
//            "actions.request_magic_link",
//            Modifier
//                .actionHeight()
//                .listItemPadding(),
//            enabled = isNotBusy,
//            action = openEmailMagicLink,
//        )

        LinkAction(
            "invitationSignup.forgot_password",
            Modifier
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
        LinkAction(
            "actions.back",
            modifier = Modifier
                .listItemPadding()
                .testTag("loginCancelBtn"),
            arrangement = Arrangement.Start,
            enabled = isNotBusy,
            action = closeAuthentication,
        )
    } else {
        LoginWithDifferentMethod(
            onClick = onBack,
        )
    }
}

@Composable
internal fun AuthenticatedScreen(
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

@DayNightPreviews
@Composable
fun LogoRowPreview() {
    CrisisCleanupTheme {
        CrisisCleanupLogoRow()
    }
}

@DayNightPreviews
@Composable
fun LoginWithDifferentMethodPreview() {
    CrisisCleanupTheme {
        LoginWithDifferentMethod(
            onClick = {},
        )
    }
}
