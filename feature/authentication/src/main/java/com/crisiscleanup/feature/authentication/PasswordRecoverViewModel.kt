package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PasswordRecoverViewModel @Inject constructor(
    accountDataRepository: AccountDataRepository,
    private val inputValidator: InputValidator,
    private val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
) : ViewModel() {
    val emailAddress = MutableStateFlow<String?>(null)
    val forgotPasswordErrorMessage = MutableStateFlow("")
    val magicLinkErrorMessage = MutableStateFlow("")

    // TODO Clear password values on view close
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    val resetPasswordErrorMessage = MutableStateFlow("")
    val resetPasswordConfirmErrorMessage = MutableStateFlow("")

    private val isInitiatingPasswordReset = MutableStateFlow(false)
    private val isInitiatingMagicLink = MutableStateFlow(false)
    private val isResettingPassword = MutableStateFlow(false)

    val isBusy = combine(
        isInitiatingPasswordReset,
        isInitiatingMagicLink,
        isResettingPassword,
        ::Triple,
    )
        .map { (b0, b1, b2) -> b0 || b1 || b2 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val isPasswordResetInitiated = MutableStateFlow(false)
    val isMagicLinkInitiated = MutableStateFlow(false)
    val isPasswordReset = MutableStateFlow(false)

    init {
        accountDataRepository.accountData
            .onEach {
                if (emailAddress.value?.isNotBlank() != true) {
                    emailAddress.value = it.emailAddress
                }
            }
            .launchIn(viewModelScope)
    }

    fun clearState() {
        password = ""
        confirmPassword = ""
    }

    fun onInitiatePasswordReset() {
        forgotPasswordErrorMessage.value = ""

        val email = emailAddress.value ?: ""
        if (email.isBlank() ||
            !inputValidator.validateEmailAddress(email)
        ) {
            forgotPasswordErrorMessage.value = translator("invitationSignup.email_error")
            return
        }

        // TODO Request password

        // TODO: Only on success
        logger.logDebug("Request password on $email")
        isPasswordResetInitiated.value = true
    }

    fun onInitiateMagicLink() {
        magicLinkErrorMessage.value = ""

        val email = emailAddress.value ?: ""
        if (email.isBlank() ||
            !inputValidator.validateEmailAddress(email)
        ) {
            magicLinkErrorMessage.value = translator("invitationSignup.email_error")
            return
        }

        // TODO Request magic link. Set success on success

        // TODO: Only on success
        logger.logDebug("Request magic link")
        isMagicLinkInitiated.value = true
    }

    fun clearResetPasswordErrors() {
        resetPasswordErrorMessage.value = ""
        resetPasswordConfirmErrorMessage.value = ""
    }

    fun onResetPassword() {
        clearResetPasswordErrors()

        val pw = password
        val confirmPw = confirmPassword

        if (pw.trim().length < 8) {
            resetPasswordErrorMessage.value =
                translator("invitationSignup.password_length_error")
            return
        }
        if (pw != confirmPw) {
            resetPasswordConfirmErrorMessage.value =
                translator("resetPassword.mismatch_passwords_try_again")
            return
        }

        logger.logDebug(("Reset password"))

        // TODO: Only on success
        clearState()
        isPasswordReset.value = true
    }
}