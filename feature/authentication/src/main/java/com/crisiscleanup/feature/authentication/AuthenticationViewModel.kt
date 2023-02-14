package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
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
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val authApiClient: CrisisCleanupAuthApi,
    private val inputValidator: InputValidator,
    private val accessTokenDecoder: AccessTokenDecoder,
    private val authEventManager: AuthEventManager,
    private val logger: AppLogger,
    private val resProvider: AndroidResourceProvider,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var isAuthenticating = MutableStateFlow(false)
    val isNotAuthenticating = isAuthenticating.map { !it }
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed()
        )

    val uiState: StateFlow<AuthenticateScreenUiState> =
        accountDataRepository.accountData.map {
            if (loginInputData.emailAddress.isEmpty() &&
                it.emailAddress.isNotEmpty()
            ) {
                loginInputData.emailAddress = it.emailAddress
                loginInputData.password = ""
            }

            AuthenticateScreenUiState.Ready(
                authenticationState = AuthenticationState(
                    accountData = it,
                    hasAccessToken = it.accessToken.isNotEmpty(),
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            initialValue = AuthenticateScreenUiState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

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

    fun clearErrorVisuals() {
        isInvalidEmail.value = false
        isInvalidPassword.value = false
        errorMessage.value = ""
    }

    private fun validateAuthInput(emailAddress: String, password: String): Boolean {
        if (!inputValidator.validateEmailAddress(emailAddress)) {
            errorMessage.value = resProvider.getString(R.string.enter_valid_email)
            isInvalidEmail.value = true
            return false
        }

        if (password.isEmpty()) {
            errorMessage.value = resProvider.getString(R.string.enter_valid_password)
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
                val hasError = (result.errors?.size ?: 0) > 0
                if (hasError) {
                    errorMessage.value = resProvider.getString(R.string.error_during_authentication)

                    val logErrorMessage = result.errors!![0].message?.get(0) ?: "Server error"
                    logger.logException(Exception(logErrorMessage))
                } else {
                    val accessToken = result.accessToken!!

                    val expirySeconds: Long =
                        accessTokenDecoder.decode(accessToken).expiresAt.epochSeconds

                    val claims = result.claims!!
                    val profilePicUri: String = claims.files?.firstOrNull {
                        it.fileTypeT == "fileTypes.user_profile_picture"
                    }?.largeThumbnailUrl ?: ""
                    accountDataRepository.setAccount(
                        id = claims.id,
                        accessToken = accessToken,
                        email = claims.email,
                        firstName = claims.firstName,
                        lastName = claims.lastName,
                        expirySeconds = expirySeconds,
                        profilePictureUri = profilePicUri,
                    )
                }
            } catch (e: Exception) {
                var isInvalidCredentials = false
                if (e is HttpException) {
                    isInvalidCredentials = e.code() == 400
                }

                if (isInvalidCredentials) {
                    errorMessage.value = resProvider.getString(R.string.invalid_credentials_retry)
                } else {
                    errorMessage.value = resProvider.getString(R.string.error_during_authentication)
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

        isAuthenticating.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                authApiClient.logout()

                loginInputData.apply {
                    emailAddress = ""
                    password = ""
                }
                authEventManager.onLogout()
            } catch (e: Exception) {
                errorMessage.value = resProvider.getString(R.string.error_during_authentication)
                logger.logException(e)
            } finally {
                isAuthenticating.value = false
            }
        }
    }
}

sealed interface AuthenticateScreenUiState {
    object Loading : AuthenticateScreenUiState
    data class Ready(val authenticationState: AuthenticationState) : AuthenticateScreenUiState
}