package com.crisiscleanup.feature.authentication

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
class ForgotPasswordViewModel @Inject constructor(
    accountDataRepository: AccountDataRepository,
    private val inputValidator: InputValidator,
    private val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
) : ViewModel() {
    val emailAddress = MutableStateFlow<String?>(null)
    val forgotPasswordErrorMessage = MutableStateFlow("")
    val magicLinkErrorMessage = MutableStateFlow("")

    private val isResettingPassword = MutableStateFlow(false)
    private val isRequestingMagicLink = MutableStateFlow(false)

    val isBusy = combine(
        isResettingPassword,
        isRequestingMagicLink,
        ::Pair,
    )
        .map { (b0, b1) -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val isMagicLinkRequested = MutableStateFlow(false)

    init {
        accountDataRepository.accountData
            .onEach {
                if (emailAddress.value?.isNotBlank() != true) {
                    emailAddress.value = it.emailAddress
                }
            }
            .launchIn(viewModelScope)
    }

    fun onRequestPassword() {
        forgotPasswordErrorMessage.value = ""

        val email = emailAddress.value ?: ""
        if (email.isBlank() ||
            !inputValidator.validateEmailAddress(email)
        ) {
            forgotPasswordErrorMessage.value = translator("invitationSignup.email_error")
            return
        }

        // TODO Request password

        logger.logDebug("Request password on $email")
    }

    fun onRequestMagicLink() {
        magicLinkErrorMessage.value = ""

        val email = emailAddress.value ?: ""
        if (email.isBlank() ||
            !inputValidator.validateEmailAddress(email)
        ) {
            magicLinkErrorMessage.value = translator("invitationSignup.email_error")
            return
        }

        // TODO Request magic link. Set success on success

        logger.logDebug("Request magic link")

        isMagicLinkRequested.value = true
    }
}