package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupLogoRow
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarCancelAction
import com.crisiscleanup.core.designsystem.component.roundedOutline
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.core.designsystem.theme.primaryRedColor
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.AuthenticateScreenViewState
import com.crisiscleanup.feature.authentication.LoginWithPhoneViewModel
import com.crisiscleanup.feature.authentication.PhoneNumberAccount
import com.crisiscleanup.feature.authentication.R
import com.crisiscleanup.feature.authentication.model.AuthenticationState

@Composable
fun LoginWithPhoneRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAuthenticated: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
    viewModel: LoginWithPhoneViewModel = hiltViewModel(),
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
                    if (viewModel.openPhoneCodeLogin) {
                        val onPhoneCodeBack =
                            remember(viewModel) { { viewModel.openPhoneCodeLogin = false } }
                        VerifyPhoneCodeScreen(
                            onBack = onPhoneCodeBack,
                        )
                    } else {
                        AnimatedVisibility(visible = !isKeyboardOpen) {
                            CrisisCleanupLogoRow()
                        }
                        LoginWithPhoneScreen(
                            authState,
                            onBack = onBack,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginWithPhoneScreen(
    authState: AuthenticationState,
    onBack: () -> Unit = {},
    viewModel: LoginWithPhoneViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current

    Text(
        modifier = listItemModifier.testTag("phoneLoginHeaderText"),
        text = t("actions.login", R.string.login),
        style = LocalFontStyles.current.header1,
    )

    ConditionalErrorMessage(viewModel.errorMessage, "phoneLogin")

    val isRequestingCode by viewModel.isRequestingCode.collectAsStateWithLifecycle()
    val isNotBusy = !isRequestingCode

    val phoneNumber by viewModel.phoneNumberInput.collectAsStateWithLifecycle()
    val focusPhone = phoneNumber.isEmpty()
    val updatePhoneInput =
        remember(viewModel) { { s: String -> viewModel.phoneNumberInput.value = s } }
    val requestPhoneCode = remember(viewModel, phoneNumber) {
        {
            viewModel.requestPhoneCode(phoneNumber)
        }
    }
    OutlinedClearableTextField(
        modifier = fillWidthPadded.testTag("phoneLoginTextField"),
        label = t("loginWithPhone.enter_cell"),
        value = phoneNumber,
        onValueChange = updatePhoneInput,
        keyboardType = KeyboardType.Phone,
        enabled = isNotBusy,
        isError = false,
        hasFocus = focusPhone,
        imeAction = ImeAction.Done,
        onEnter = requestPhoneCode,
    )

    // TODO Hide if device does not have a SIM/phone number
    LinkAction(
        t("loginWithPhone.use_phones_number"),
        modifier = Modifier
            .listItemPadding()
            .testTag("phoneLoginRequestPhoneNumber"),
        arrangement = Arrangement.End,
        enabled = true,
        action = viewModel::requestPhoneNumber,
    )

    BusyButton(
        modifier = fillWidthPadded.testTag("phoneLoginAction"),
        onClick = requestPhoneCode,
        enabled = isNotBusy,
        text = t("loginForm.login_with_cell"),
        indicateBusy = isRequestingCode,
    )

    if (authState.hasAuthenticated) {
        LinkAction(
            "actions.back",
            modifier = Modifier
                .listItemPadding()
                .testTag("phoneLoginBackAction"),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.VerifyPhoneCodeScreen(
    onBack: () -> Unit = {},
    viewModel: LoginWithPhoneViewModel = hiltViewModel(),
) {
    BackHandler {
        onBack()
    }

    val t = LocalAppTranslator.current

    val isExchangingCode by viewModel.isExchangingCode.collectAsStateWithLifecycle()
    val isNotBusy = !isExchangingCode

    val isNotSelectAccount = !viewModel.isSelectAccount

    val closeKeyboard = rememberCloseKeyboard(viewModel)

    TopAppBarCancelAction(
        title = t("actions.login"),
        onAction = onBack,
    )

    ConditionalErrorMessage(viewModel.errorMessage, "verifyPhoneCode")

    val obfuscatedPhoneNumber by viewModel.obfuscatedPhoneNumber.collectAsStateWithLifecycle()
    val codeSize = 6
    Column(
        Modifier
            .listItemHorizontalPadding()
            .listItemTopPadding(),
    ) {
        Text(
            t("loginWithPhone.enter_x_digit_code")
                // TODO Configure replacing text/number
                .replace("{codeCount}", "$codeSize"),
            Modifier.testTag("verifyPhoneCodeInstruction"),
        )
        Text(obfuscatedPhoneNumber)
    }

    var focusPhoneCode by remember { mutableStateOf(false) }
    val submitCode = remember(viewModel) {
        {
            val enteredCode = viewModel.phoneCode.trim()
            // Give room for future changes
            if (enteredCode.length >= codeSize - 1) {
                viewModel.authenticate(enteredCode)
                focusPhoneCode = false
                closeKeyboard()
            } else {
                viewModel.onIncompleteCode()
                focusPhoneCode = true
            }
        }
    }
    val updatePhoneCode = remember(viewModel) { { s: String -> viewModel.phoneCode = s.trim() } }
    OutlinedClearableTextField(
        modifier = listItemModifier.testTag("phoneLoginCodeTextField"),
        label = t("loginWithPhone.phone_login_code"),
        value = viewModel.phoneCode.trim(),
        onValueChange = updatePhoneCode,
        keyboardType = KeyboardType.Number,
        enabled = isNotBusy && isNotSelectAccount,
        isError = false,
        hasFocus = focusPhoneCode,
        imeAction = ImeAction.Done,
        onEnter = submitCode,
    )

    val phoneNumber by viewModel.phoneNumberNumbers.collectAsStateWithLifecycle()
    val requestPhoneCode = remember(phoneNumber, viewModel) {
        {
            viewModel.requestPhoneCode(phoneNumber)
        }
    }
    LinkAction(
        "actions.resend_code",
        modifier = Modifier
            .listItemPadding()
            .testTag("resendPhoneCodeBtn"),
        arrangement = Arrangement.End,
        enabled = isNotBusy,
        action = requestPhoneCode,
    )

    if (viewModel.showMultiPhoneToggle) {
        LinkAction(
            "Toggle account select",
            modifier = Modifier
                .padding(16.dp)
                .testTag("phoneLoginToggleAccountSelect"),
            arrangement = Arrangement.End,
            enabled = true,
            color = primaryOrangeColor,
        ) {
            viewModel.toggleMultiPhone()
        }
    }

    val accountOptions = viewModel.accountOptions.toList()
    if (accountOptions.size > 1) {
        Text(
            t("loginWithPhone.phone_associated_multiple_users"),
            modifier = listItemModifier,
        )

        Text(
            t("actions.select_account"),
            modifier = Modifier
                .listItemHorizontalPadding(),
            style = LocalFontStyles.current.header4,
        )

        Box(Modifier.fillMaxWidth()) {
            var contentSize by remember { mutableStateOf(Size.Zero) }
            var showDropdown by remember { mutableStateOf(false) }
            Column(
                Modifier
                    .clickable(
                        onClick = { showDropdown = !showDropdown },
                        enabled = isNotBusy,
                    )
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        contentSize = it.size.toSize()
                    }
                    .then(listItemModifier),
            ) {
                val selectedOption by viewModel.selectedAccount.collectAsStateWithLifecycle()
                Row(
                    Modifier
                        .listItemVerticalPadding()
                        .roundedOutline()
                        // TODO Common dimensions
                        .padding(8.dp)
                        .testTag("phoneLoginAccountSelect"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val optionTextColor = if (selectedOption.accountDisplay.isBlank()) {
                        primaryRedColor
                    } else {
                        LocalContentColor.current
                    }
                    Text(
                        selectedOption.accountDisplay.ifBlank { t("actions.select_account") },
                        Modifier.weight(1f),
                        color = optionTextColor,
                    )
                    var tint = LocalContentColor.current
                    if (!isNotBusy) {
                        tint = tint.disabledAlpha()
                    }
                    Icon(
                        imageVector = CrisisCleanupIcons.UnfoldMore,
                        contentDescription = t("actions.select_account"),
                        tint = tint,
                    )
                }
            }

            val onSelect = { account: PhoneNumberAccount ->
                viewModel.selectedAccount.value = account
                showDropdown = false
            }
            DropdownMenu(
                modifier = Modifier
                    .width(
                        with(LocalDensity.current) {
                            contentSize.width.toDp().minus(listItemDropdownMenuOffset.x.times(2))
                        },
                    ),
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                offset = listItemDropdownMenuOffset,
            ) {
                for (option in accountOptions) {
                    key(option.userId) {
                        DropdownMenuItem(
                            modifier = Modifier.optionItemHeight(),
                            text = {
                                Text(
                                    option.accountDisplay,
                                    style = LocalFontStyles.current.header4,
                                )
                            },
                            onClick = { onSelect(option) },
                        )
                    }
                }
            }
        }
    }

    // TODO Move button above the screen keyboard (when visible)
    Spacer(Modifier.weight(1f))

    BusyButton(
        modifier = fillWidthPadded.testTag("verifyPhoneCodeAction"),
        onClick = submitCode,
        enabled = isNotBusy,
        text = t("actions.submit"),
        indicateBusy = isExchangingCode,
    )
}
