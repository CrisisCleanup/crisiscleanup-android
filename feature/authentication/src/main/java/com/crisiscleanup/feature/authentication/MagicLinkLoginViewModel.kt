package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class MagicLinkLoginViewModel @Inject constructor(
    accountDataRepository: AccountDataRepository,
    authApi: CrisisCleanupAuthApi,
    dataApi: CrisisCleanupNetworkDataSource,
    private val translator: KeyResourceTranslator,
    private val authEventBus: AuthEventBus,
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
                val loginCode = authEventBus.emailLoginCodes.first()
                if (loginCode.isNotBlank()) {
                    val tokens = authApi.magicLinkLogin(loginCode)
                    tokens.accessToken?.let { accessToken ->
                        val refreshToken = tokens.refreshToken!!
                        val expiresIn = tokens.expiresIn!!

                        dataApi.getProfile(accessToken)?.let { accountProfile ->
                            val accountData = accountDataRepository.accountData.first()
                            val emailAddress = accountData.emailAddress
                            if (emailAddress.isNotBlank() && emailAddress != accountProfile.email) {
                                message =
                                    translator.translate(
                                        "~~Logging in with an account different from the currently signed in account is not supported. Logout of the signed in account first then login with a different account.",
                                        0,
                                    )

                                // TODO Clear account data and support logging in with different email address?
                            } else {
                                val expirySeconds =
                                    Clock.System.now().plus(expiresIn.seconds).epochSeconds
                                accountDataRepository.setAccount(
                                    refreshToken = refreshToken,
                                    accessToken = accessToken,
                                    id = accountProfile.id,
                                    email = accountProfile.email,
                                    firstName = accountProfile.firstName,
                                    lastName = accountProfile.lastName,
                                    expirySeconds = expirySeconds,
                                    profilePictureUri = accountProfile.profilePicUrl ?: "",
                                    org = OrgData(
                                        id = accountProfile.organization.id,
                                        name = accountProfile.organization.name,
                                    ),
                                )

                                isAuthenticateSuccessful.value = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isAuthenticating.value = false
            }

            if (!isAuthenticateSuccessful.value) {
                errorMessage = message.ifBlank {
                    translator("~~Magic link is invalid. Request another magic link.", 0)
                }
            }
        }
    }

    fun clearMagicLinkLogin() {
        authEventBus.onEmailLoginLink("")
    }
}
