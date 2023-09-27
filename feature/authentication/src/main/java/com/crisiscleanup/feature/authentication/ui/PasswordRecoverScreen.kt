package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.OutlinedObfuscatingTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.primaryRedColor
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.PasswordRecoverViewModel
import com.crisiscleanup.feature.authentication.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecoverRoute(
    onBack: () -> Unit,
    viewModel: PasswordRecoverViewModel = hiltViewModel(),
    showForgotPassword: Boolean = false,
    showResetPassword: Boolean = false,
    showMagicLink: Boolean = false,
) {
    val translator = LocalAppTranslator.current
    var titleKey = "nav.magic_link"
    if (showForgotPassword) {
        titleKey = "invitationSignup.forgot_password"
    } else if (showResetPassword) {
        titleKey = "actions.reset_password"
    }

    val emailAddress by viewModel.emailAddress.collectAsStateWithLifecycle()
    val emailAddressNn = emailAddress ?: ""

    val isNotLoading = emailAddress != null
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val clearStateOnBack = remember(viewModel) {
        {
            viewModel.clearState()
            onBack()
        }
    }
    BackHandler(!isBusy) {
        clearStateOnBack()
    }

    val closeKeyboard = rememberCloseKeyboard(viewModel)

    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBarBackAction(
            title = translator(titleKey),
            onAction = clearStateOnBack,
            modifier = Modifier.testTag("forgotPasswordBackBtn"),
        )

        val isResetInitiated by viewModel.isPasswordResetInitiated.collectAsStateWithLifecycle()
        val isPasswordReset by viewModel.isPasswordReset.collectAsStateWithLifecycle()
        val isMagicLinkInitiated by viewModel.isMagicLinkInitiated.collectAsStateWithLifecycle()
        if (isResetInitiated) {
            PasswordResetInitiatedView()
        } else if (isPasswordReset) {
            PasswordResetSuccessfulView()
        } else if (isMagicLinkInitiated) {
            MagicLinkInitiatedView()
        } else {
            if (showForgotPassword) {
                ForgotPasswordView(
                    emailAddress = emailAddressNn,
                    isEditable = isNotLoading,
                    isBusy = isBusy,
                )

                Spacer(Modifier.height(32.dp))
            }

            if (showResetPassword) {
                val resetToken by viewModel.resetPasswordToken.collectAsStateWithLifecycle()
                if (resetToken.isBlank()) {
                    PasswordResetNotPossibleView()
                } else {
                    ResetPasswordView(
                        isEditable = isNotLoading,
                        isBusy = isBusy,
                    )
                }

                Spacer(Modifier.height(32.dp))
            }

//            if (showMagicLink) {
//                MagicLinkView(
//                    emailAddress = emailAddressNn,
//                    isEditable = isNotLoading,
//                    isBusy = isBusy,
//                )
//            }
        }
    }
}

@Composable
private fun ForgotPasswordView(
    viewModel: PasswordRecoverViewModel = hiltViewModel(),
    emailAddress: String = "",
    isEditable: Boolean = false,
    isBusy: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    Text(
        translator("resetPassword.forgot_your_password_or_reset"),
        listItemModifier,
        style = LocalFontStyles.current.header3,
    )

    Text(
        translator("resetPassword.enter_email_for_reset_instructions"),
        listItemModifier,
    )

    val updateEmailInput = remember(viewModel) {
        { s: String ->
            viewModel.emailAddress.value = s
        }
    }
    val emailErrorMessage by viewModel.forgotPasswordErrorMessage.collectAsStateWithLifecycle()
    val hasError = emailErrorMessage.isNotBlank()

    if (hasError) {
        Text(
            emailErrorMessage,
            listItemModifier,
            color = primaryRedColor,
        )
    }

    OutlinedClearableTextField(
        modifier = fillWidthPadded.testTag("forgotPasswordTextField"),
        label = translator("loginForm.email_placeholder", R.string.email),
        value = emailAddress,
        onValueChange = updateEmailInput,
        keyboardType = KeyboardType.Email,
        enabled = !isBusy,
        isError = hasError,
        hasFocus = hasError,
        onEnter = viewModel::onInitiatePasswordReset,
        imeAction = ImeAction.Done,
    )

    BusyButton(
        modifier = fillWidthPadded.testTag("forgotPasswordBtn"),
        onClick = viewModel::onInitiatePasswordReset,
        enabled = isEditable,
        text = translator("actions.reset_password"),
        indicateBusy = isBusy,
    )
}

@Composable
private fun PasswordResetInitiatedView() {
    val translator = LocalAppTranslator.current

    Text(
        translator("resetPassword.email_arrive_soon_check_junk"),
        listItemModifier,
        style = LocalFontStyles.current.header3,
    )
}

@Composable
private fun ResetPasswordView(
    viewModel: PasswordRecoverViewModel = hiltViewModel(),
    isEditable: Boolean = false,
    isBusy: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    Text(
        translator("nav.reset_password"),
        listItemModifier,
        style = LocalFontStyles.current.header3,
    )

    Text(
        translator("resetPassword.enter_new_password"),
        listItemModifier,
    )

    var isObfuscatingPassword by remember { mutableStateOf(true) }
    var isObfuscatingConfirmPassword by remember { mutableStateOf(true) }
    val updatePasswordInput = remember(viewModel) {
        { s: String -> viewModel.password = s }
    }
    val updateConfirmPasswordInput = remember(viewModel) {
        { s: String -> viewModel.confirmPassword = s }
    }
    val passwordErrorMessage by viewModel.resetPasswordErrorMessage.collectAsStateWithLifecycle()
    val confirmPasswordErrorMessage by viewModel.resetPasswordConfirmErrorMessage.collectAsStateWithLifecycle()
    val hasPasswordError = passwordErrorMessage.isNotBlank()
    val hasConfirmPasswordError = confirmPasswordErrorMessage.isNotBlank()

    val errorMessage = passwordErrorMessage.ifBlank { confirmPasswordErrorMessage }
    if (errorMessage.isNotBlank()) {
        Text(
            errorMessage,
            listItemModifier,
            color = primaryRedColor,
        )
    }

    OutlinedObfuscatingTextField(
        modifier = fillWidthPadded.testTag("resetPasswordTextField"),
        label = translator("resetPassword.password"),
        value = viewModel.password,
        onValueChange = updatePasswordInput,
        isObfuscating = isObfuscatingPassword,
        onObfuscate = { isObfuscatingPassword = !isObfuscatingPassword },
        enabled = !isBusy,
        isError = hasPasswordError,
        hasFocus = hasPasswordError,
        onNext = viewModel::clearResetPasswordErrors,
        imeAction = ImeAction.Next,
    )

    OutlinedObfuscatingTextField(
        modifier = fillWidthPadded.testTag("resetPasswordConfirmTextField"),
        label = translator("resetPassword.confirm_password"),
        value = viewModel.confirmPassword,
        onValueChange = updateConfirmPasswordInput,
        isObfuscating = isObfuscatingConfirmPassword,
        onObfuscate = { isObfuscatingConfirmPassword = !isObfuscatingConfirmPassword },
        enabled = !isBusy,
        isError = hasConfirmPasswordError,
        hasFocus = hasConfirmPasswordError,
        onEnter = viewModel::onResetPassword,
        imeAction = ImeAction.Done,
    )

    BusyButton(
        modifier = fillWidthPadded.testTag("resetPasswordBtn"),
        onClick = viewModel::onResetPassword,
        enabled = isEditable,
        text = translator("actions.reset"),
        indicateBusy = isBusy,
    )
}

@Composable
private fun PasswordResetNotPossibleView() {
    val translator = LocalAppTranslator.current

    Text(
        translator("resetPassword.password_reset_not_possible"),
        listItemModifier,
    )
}

@Composable
private fun PasswordResetSuccessfulView() {
    val translator = LocalAppTranslator.current

    Text(
        translator("resetPassword.password_reset"),
        listItemModifier,
        style = LocalFontStyles.current.header3,
    )
}

@Composable
private fun MagicLinkView(
    viewModel: PasswordRecoverViewModel = hiltViewModel(),
    emailAddress: String = "",
    isEditable: Boolean = false,
    isBusy: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    Text(
        translator("actions.request_magic_link"),
        listItemModifier,
        style = LocalFontStyles.current.header3,
    )

    Text(
        translator("magicLink.magic_link_description"),
        listItemModifier,
    )

    val updateEmailInput = remember(viewModel) {
        { s: String ->
            viewModel.emailAddress.value = s
        }
    }
    val emailErrorMessage by viewModel.magicLinkErrorMessage.collectAsStateWithLifecycle()
    val hasError = emailErrorMessage.isNotBlank()

    if (emailErrorMessage.isNotBlank()) {
        Text(
            emailErrorMessage,
            listItemModifier,
            color = primaryRedColor,
        )
    }

    OutlinedClearableTextField(
        modifier = fillWidthPadded.testTag("magicLinkTextField"),
        label = translator("loginForm.email_placeholder", R.string.email),
        value = emailAddress,
        onValueChange = updateEmailInput,
        keyboardType = KeyboardType.Email,
        enabled = !isBusy,
        isError = hasError,
        hasFocus = hasError,
        onEnter = viewModel::onInitiateMagicLink,
        imeAction = ImeAction.Done,
    )

    BusyButton(
        modifier = fillWidthPadded.testTag("emailMagicLinkBtn"),
        onClick = viewModel::onInitiateMagicLink,
        enabled = isEditable,
        text = translator("actions.submit"),
        indicateBusy = isBusy,
    )
}

@Composable
private fun MagicLinkInitiatedView() {
    val translator = LocalAppTranslator.current

    Text(
        translator("magicLink.magic_link_sent"),
        listItemModifier,
        style = LocalFontStyles.current.header3,
    )
}
