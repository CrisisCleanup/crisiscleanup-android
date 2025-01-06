package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.OutlinedObfuscatingTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.PasswordRecoverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordRoute(
    onBack: () -> Unit = {},
    closeResetPassword: () -> Unit = {},
    viewModel: PasswordRecoverViewModel = hiltViewModel(),
) {
    val isPasswordReset by viewModel.isPasswordReset.collectAsStateWithLifecycle()

    val clearStateOnBack = remember(viewModel, isPasswordReset, onBack, closeResetPassword) {
        {
            viewModel.clearResetPassword()

            if (isPasswordReset) {
                closeResetPassword()
            } else {
                onBack()
            }
        }
    }

    BackHandler {
        clearStateOnBack()
    }

    val t = LocalAppTranslator.current

    val closeKeyboard = rememberCloseKeyboard()

    val emailAddress by viewModel.emailAddress.collectAsStateWithLifecycle()
    val isEmailDefined = emailAddress != null
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBarBackAction(
            title = t("actions.reset_password"),
            onAction = clearStateOnBack,
            modifier = Modifier.testTag("passwordRecoverBackBtn"),
        )

        if (isPasswordReset) {
            PasswordResetSuccessfulView()
        } else {
            val resetToken by viewModel.resetPasswordToken.collectAsStateWithLifecycle()
            if (resetToken.isBlank()) {
                PasswordResetNotPossibleView()
                // TODO Account reset password requires back 2x likely due to clear state
                // clearStateOnBack()
            } else {
                ResetPasswordView(
                    isEditable = isEmailDefined,
                    isBusy = isBusy,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
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
private fun PasswordResetNotPossibleView() {
    val translator = LocalAppTranslator.current

    Text(
        translator("resetPassword.password_reset_not_possible"),
        listItemModifier,
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
            color = MaterialTheme.colorScheme.error,
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
