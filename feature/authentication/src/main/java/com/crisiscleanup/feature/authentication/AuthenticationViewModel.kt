package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyOrgData
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.NetworkFile
import com.crisiscleanup.core.network.model.condenseMessages
import com.crisiscleanup.feature.authentication.model.AuthenticationState
import com.crisiscleanup.feature.authentication.model.LoginInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val authApiClient: CrisisCleanupAuthApi,
    private val inputValidator: InputValidator,
    private val authEventBus: AuthEventBus,
    private val translator: KeyResourceTranslator,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
) : ViewModel() {
    private var isAuthenticating = MutableStateFlow(false)
    val isNotAuthenticating = isAuthenticating.map(Boolean::not).stateIn(
        scope = viewModelScope,
        initialValue = true,
        started = SharingStarted.WhileSubscribed(),
    )

    val uiState: StateFlow<AuthenticateScreenUiState> = accountDataRepository.accountData.map {
        if (loginInputData.emailAddress.isEmpty() && it.emailAddress.isNotEmpty()) {
            loginInputData.emailAddress = it.emailAddress
            loginInputData.password = ""
        }

        AuthenticateScreenUiState.Ready(
            authenticationState = AuthenticationState(accountData = it),
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = AuthenticateScreenUiState.Loading,
        started = SharingStarted.WhileSubscribed(),
    )

    val isAuthenticateSuccessful = MutableStateFlow(false)

    val loginInputData = LoginInputData()

    /**
     * General error message during authentication
     */
    var errorMessage = mutableStateOf("")
        private set

    var isInvalidEmail = mutableStateOf(false)
        private set
    var isInvalidPassword = mutableStateOf(false)
        private set

    // TODO Test (if not included in other tests)
    fun onCloseScreen() {
        loginInputData.password = ""
        errorMessage.value = ""
        isInvalidEmail.value = false
        isInvalidEmail.value = false
        isAuthenticateSuccessful.value = false
    }

    fun clearErrorVisuals() {
        isInvalidEmail.value = false
        isInvalidPassword.value = false
        errorMessage.value = ""
    }

    private fun validateAuthInput(emailAddress: String, password: String): Boolean {
        if (emailAddress.isBlank()) {
            errorMessage.value = translator("invitationSignup.email_error")
            isInvalidEmail.value = true
            return false
        }

        if (!inputValidator.validateEmailAddress(emailAddress)) {
            errorMessage.value = translator("invitationSignup.email_error")
            isInvalidEmail.value = true
            return false
        }

        if (password.isEmpty()) {
            errorMessage.value = translator("invitationSignup.password_length_error")
            isInvalidPassword.value = true
            return false
        }

        return true
    }

    // TODO Finish writing tests
    fun authenticateEmailPassword() {
        if (isAuthenticating.value) {
            return
        }

        clearErrorVisuals()

        val emailAddress = loginInputData.emailAddress
        val password = loginInputData.password
        // TODO Test coverage
        if (!validateAuthInput(emailAddress, password)) {
            return
        }

        isAuthenticating.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                val result = authApiClient.login(emailAddress, password)
                val oauthResult = authApiClient.oauthLogin(emailAddress, password)
                val hasError = result.errors?.isNotEmpty() == true
                if (hasError) {
                    errorMessage.value = translator("info.unknown_error")

                    val logErrorMessage =
                        result.errors?.condenseMessages?.ifBlank { "Server error" }
                    logger.logException(Exception(logErrorMessage))
                } else {
                    val refreshToken = oauthResult.refreshToken
                    val accessToken = oauthResult.accessToken
                    val expirySeconds = Clock.System.now().epochSeconds + oauthResult.expiresIn

                    val claims = result.claims!!
                    val profilePicUri =
                        claims.files?.firstOrNull(NetworkFile::isProfilePicture)?.largeThumbnailUrl
                            ?: ""

                    // TODO Test coverage
                    val organization = result.organizations
                    val orgData =
                        if (organization?.isActive == true && organization.id >= 0 && organization.name.isNotEmpty()) {
                            OrgData(
                                organization.id,
                                organization.name,
                            )
                        } else {
                            emptyOrgData
                        }

                    accountDataRepository.setAccount(
                        refreshToken = refreshToken,
                        accessToken = accessToken,
                        id = claims.id,
                        email = claims.email,
                        firstName = claims.firstName,
                        lastName = claims.lastName,
                        expirySeconds = expirySeconds,
                        profilePictureUri = profilePicUri,
                        org = orgData,
                    )

                    isAuthenticateSuccessful.value = true
                }
            } catch (e: Exception) {
                var isInvalidCredentials = false
                if (e is CrisisCleanupNetworkException) {
                    isInvalidCredentials = e.statusCode == 400
                }

                if (isInvalidCredentials) {
                    errorMessage.value = translator("loginForm.invalid_credentials_msg")
                } else {
                    errorMessage.value = translator("info.unknown_error")
                    logger.logException(e)
                }
            } finally {
                isAuthenticating.value = false
            }
        }
    }

    // TODO Finish writing tests
    fun logout() {
        if (isAuthenticating.value) {
            return
        }

        clearErrorVisuals()

        loginInputData.apply {
            emailAddress = ""
            password = ""
        }
        authEventBus.onLogout()
    }
}

sealed interface AuthenticateScreenUiState {
    object Loading : AuthenticateScreenUiState
    data class Ready(val authenticationState: AuthenticationState) : AuthenticateScreenUiState
}
