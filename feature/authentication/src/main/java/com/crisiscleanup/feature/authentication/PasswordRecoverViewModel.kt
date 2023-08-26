package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AccountUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class PasswordRecoverViewModel @Inject constructor(
    accountDataRepository: AccountDataRepository,
    private val accountUpdateRepository: AccountUpdateRepository,
    private val inputValidator: InputValidator,
    private val translator: KeyResourceTranslator,
    authEventBus: AuthEventBus,
    @Logger(CrisisCleanupLoggers.Account) private val logger: AppLogger,
) : ViewModel() {
    val emailAddress = MutableStateFlow<String?>(null)
    val forgotPasswordErrorMessage = MutableStateFlow("")
    val magicLinkErrorMessage = MutableStateFlow("")

    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    val resetPasswordErrorMessage = MutableStateFlow("")
    val resetPasswordConfirmErrorMessage = MutableStateFlow("")

    val resetPasswordToken = authEventBus.resetPasswords

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

        isInitiatingPasswordReset.value = true
        viewModelScope.launch {
            try {
                val result = accountUpdateRepository.initiatePasswordReset(email)

                var isInitiated = false
                result.expiresAt?.let {
                    if (it > Clock.System.now()) {
                        isInitiated = true
                    }
                }

                if (isInitiated) {
                    isPasswordResetInitiated.value = true
                } else {
                    forgotPasswordErrorMessage.value = result.errorMessage.ifBlank {
                        translator("~~There was an issue with starting the reset password process. Try again later.")
                    }
                }
            } finally {
                isInitiatingPasswordReset.value = false
            }
        }
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

        isInitiatingMagicLink.value = true
        viewModelScope.launch {
            try {
                val isInitiated = accountUpdateRepository.initiateEmailMagicLink(email)

                if (isInitiated) {
                    isMagicLinkInitiated.value = true
                } else {
                    magicLinkErrorMessage.value =
                        translator("~~There was an issue with sending a magic link. Try again later.")
                }
            } finally {
                isInitiatingMagicLink.value = false
            }
        }
    }

    fun clearResetPasswordErrors() {
        resetPasswordErrorMessage.value = ""
        resetPasswordConfirmErrorMessage.value = ""
    }

    fun onResetPassword() {
        val resetToken = resetPasswordToken.value
        if (resetToken.isBlank()) {
            return
        }

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

        isResettingPassword.value = true
        viewModelScope.launch {
            try {
                val isChanged = accountUpdateRepository.changePassword(pw, resetToken)

                if (isChanged) {
                    isPasswordReset.value = true
                    clearState()
                } else {
                    resetPasswordErrorMessage.value =
                        translator("~~There was an issue with resetting password. Try again later.")
                }
            } finally {
                isResettingPassword.value = false
            }
        }
    }
}