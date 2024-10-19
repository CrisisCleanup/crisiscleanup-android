package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MagicLinkLoginViewModel @Inject constructor(
    private val authApi: CrisisCleanupAuthApi,
    private val dataApi: CrisisCleanupNetworkDataSource,
    private val accountDataRepository: AccountDataRepository,
    private val accountEventBus: AccountEventBus,
    private val translator: KeyResourceTranslator,
    private val externalEventBus: ExternalEventBus,
    @Dispatcher(CrisisCleanupDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Account) private val logger: AppLogger,
) : ViewModel() {
    var errorMessage by mutableStateOf("")
        private set

    val isAuthenticating = MutableStateFlow(true)
    val isAuthenticateSuccessful = MutableStateFlow(false)

    init {
        viewModelScope.launch(ioDispatcher) {
            var message = ""
            try {
                val loginCode = externalEventBus.emailLoginCodes.first()
                val (isSuccessful, authErrorMessage) = attemptAuthentication(loginCode)
                if (isSuccessful) {
                    isAuthenticateSuccessful.value = true
                }
                message = authErrorMessage
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isAuthenticating.value = false
            }

            if (!isAuthenticateSuccessful.value) {
                errorMessage = message.ifBlank {
                    translator("magicLink.invalid_link", 0)
                }
            }
        }
    }

    private suspend fun attemptAuthentication(
        loginCode: String,
    ): Pair<Boolean, String> {
        var isSuccessful = false
        var authErrorMessage = ""

        if (loginCode.isNotBlank()) {
            val tokens = authApi.magicLinkLogin(loginCode)
            tokens.accessToken?.let { accessToken ->
                val refreshToken = tokens.refreshToken!!
                val expiresIn = tokens.expiresIn!!

                dataApi.getProfile(accessToken)?.let { accountProfile ->
                    val accountData = accountDataRepository.accountData.first()
                    val emailAddress = accountData.emailAddress
                    if (emailAddress.isNotBlank() &&
                        emailAddress.lowercase() != accountProfile.email.lowercase()
                    ) {
                        authErrorMessage = translator("magicLink.log_out_before_different_account")

                        // TODO Clear account data and support logging in with different email address?
                    } else if (accountProfile.organization.isActive == false) {
                        accountEventBus.onAccountInactiveOrganization(accountProfile.id)
                    } else {
                        accountDataRepository.setAccount(
                            accountProfile,
                            refreshToken = refreshToken,
                            accessToken,
                            expiresIn,
                        )

                        isSuccessful = true
                    }
                }
            }
        }

        return Pair(isSuccessful, authErrorMessage)
    }

    fun clearMagicLinkLogin() {
        externalEventBus.onEmailLoginLink("")
    }
}
