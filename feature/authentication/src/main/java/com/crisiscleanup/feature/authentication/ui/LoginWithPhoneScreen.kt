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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.SingleLineTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarCancelAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.AuthenticateScreenUiState
import com.crisiscleanup.feature.authentication.LoginWithPhoneViewModel
import com.crisiscleanup.feature.authentication.PhoneNumberAccount
import com.crisiscleanup.feature.authentication.R
import com.crisiscleanup.feature.authentication.model.AuthenticationState

@Composable
fun LoginWithPhoneRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
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
    val translator = LocalAppTranslator.current

    Text(
        modifier = listItemModifier.testTag("phoneLoginHeaderText"),
        text = translator("actions.login", R.string.login),
        style = LocalFontStyles.current.header1,
    )

    ConditionalErrorMessage(viewModel.errorMessage)

    val isRequestingCode by viewModel.isRequestingCode.collectAsStateWithLifecycle()
    val isNotBusy = !isRequestingCode

    val phoneNumber by viewModel.phoneNumberInput.collectAsStateWithLifecycle()
    val focusPhone = phoneNumber.isEmpty()
    val updateEmailInput =
        remember(viewModel) { { s: String -> viewModel.phoneNumberInput.value = s } }
    val requestPhoneCode = remember(viewModel, phoneNumber) {
        {
            viewModel.requestPhoneCode(phoneNumber)
        }
    }
    OutlinedClearableTextField(
        modifier = fillWidthPadded.testTag("loginPhoneTextField"),
        label = translator("~~Enter cell phone"),
        value = phoneNumber,
        onValueChange = updateEmailInput,
        keyboardType = KeyboardType.Phone,
        enabled = isNotBusy,
        isError = false,
        hasFocus = focusPhone,
        imeAction = ImeAction.Done,
        onEnter = requestPhoneCode,
    )

    BusyButton(
        modifier = fillWidthPadded.testTag("phoneLoginBtn"),
        onClick = requestPhoneCode,
        enabled = isNotBusy,
        text = translator("loginForm.login_with_cell"),
        indicateBusy = isRequestingCode,
    )

    if (authState.hasAuthenticated) {
        LinkAction(
            "actions.back",
            modifier = Modifier
                .listItemPadding()
                .testTag("phoneLoginBackBtn"),
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

    val translator = LocalAppTranslator.current

    val isExchangingCode by viewModel.isExchangingCode.collectAsStateWithLifecycle()
    val isNotBusy = !isExchangingCode

    val isSelectAccount by viewModel.isSelectAccount.collectAsStateWithLifecycle()
    val isNotSelectAccount = !isSelectAccount

    val closeKeyboard = rememberCloseKeyboard(viewModel)

    TopAppBarCancelAction(
        modifier = Modifier
            .testTag("verifyPhoneCodeBackBtn"),
        title = translator.translate("actions.login", 0),
        onAction = onBack,
    )

    ConditionalErrorMessage(viewModel.errorMessage)

    val singleCodes = viewModel.singleCodes.toList()

    val obfuscatedPhoneNumber by viewModel.obfuscatedPhoneNumber.collectAsStateWithLifecycle()
    Column(listItemModifier) {
        Text(
            translator.translate(
                "~~Enter the ${singleCodes.size} digit code we sent to",
                0,
            ),
        )
        Text(obfuscatedPhoneNumber)
    }

    var focusIndex = remember(viewModel) {
        if (singleCodes.all { it.isBlank() }) {
            0
        } else {
            -1
        }
    }
    val focusManager = LocalFocusManager.current
    val submitCode = remember(viewModel) {
        {
            val codes = viewModel.singleCodes.toList()
            val fullCode = codes
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it[it.length - 1] }
                .joinToString("")

            if (fullCode.length == codes.size) {
                viewModel.authenticate(fullCode)
                closeKeyboard()
            } else {
                viewModel.onIncompleteCode()

                codes.forEachIndexed { i, code ->
                    if (code.isBlank()) {
                        // TODO Not working as expected.
                        //      Likely needs more complicated focus measures.
                        focusIndex = i
                        return@forEachIndexed
                    }
                }
            }
        }
    }
    Row(
        listItemModifier,
        horizontalArrangement = listItemSpacedBy,
    ) {
        singleCodes.forEachIndexed { i: Int, code: String ->
            val isLastCode = i >= singleCodes.size - 1
            val onEnter = if (isLastCode) submitCode else null
            SingleLineTextField(
                modifier = Modifier.weight(1f),
                value = code,
                onValueChange = { s ->
                    // TODO Set entire code if length matches and code is blank

                    val updated = if (s.isBlank()) "" else s.last().toString()
                    viewModel.singleCodes[i] = updated

                    if (updated.isNotBlank()) {
                        val focusDirection =
                            if (isLastCode) FocusDirection.Down else FocusDirection.Next
                        focusManager.moveFocus(focusDirection)

                        if (
                            isLastCode &&
                            singleCodes.filter { it.isNotBlank() }.size >= singleCodes.size - 1
                        ) {
                            closeKeyboard()
                        }
                    }
                },
                enabled = isNotBusy && isNotSelectAccount,
                isError = false,
                drawOutline = true,
                keyboardType = KeyboardType.Number,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                hasFocus = i == focusIndex,
                imeAction = if (isLastCode) ImeAction.Done else ImeAction.Next,
                onEnter = onEnter,
            )
        }
    }

    val phoneNumber by viewModel.phoneNumberNumbers.collectAsStateWithLifecycle()
    val requestPhoneCode = remember(phoneNumber, viewModel) {
        {
            viewModel.requestPhoneCode(phoneNumber)
        }
    }
    LinkAction(
        "~~Resend Code",
        modifier = Modifier
            .listItemPadding()
            .testTag("resendPhoneCodeBtn"),
        arrangement = Arrangement.End,
        enabled = isNotBusy,
        action = requestPhoneCode,
    )

    val accountOptions = viewModel.accountOptions.toList()
    if (accountOptions.size > 1) {
        Text(
            translator("~~This phone number is associated with multiple accounts."),
            modifier = listItemModifier,
        )

        Text(
            translator("~~Select Account"),
            modifier = Modifier
                .listItemHorizontalPadding(),
            style = LocalFontStyles.current.header4,
        )

        Box(Modifier.fillMaxWidth()) {
            var contentWidth by remember { mutableStateOf(Size.Zero) }
            var showDropdown by remember { mutableStateOf(false) }
            Column(
                Modifier
                    .clickable(
                        onClick = { showDropdown = !showDropdown },
                        enabled = isNotBusy,
                    )
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        contentWidth = it.size.toSize()
                    }
                    .then(listItemModifier),
            ) {
                val selectedOption by viewModel.selectedAccount.collectAsStateWithLifecycle()
                Row(
                    Modifier.listItemVerticalPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(selectedOption.accountDisplay.ifBlank { translator("~~Select an account") })
                    Spacer(modifier = Modifier.weight(1f))
                    var tint = LocalContentColor.current
                    if (!isNotBusy) {
                        tint = tint.disabledAlpha()
                    }
                    Icon(
                        imageVector = CrisisCleanupIcons.UnfoldMore,
                        contentDescription = translator("~~Select account"),
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
                            contentWidth.width.toDp().minus(listItemDropdownMenuOffset.x.times(2))
                        },
                    ),
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
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
        modifier = fillWidthPadded.testTag("verifyPhoneCodeBtn"),
        onClick = submitCode,
        enabled = isNotBusy,
        text = translator("actions.submit"),
        indicateBusy = isExchangingCode,
    )
}