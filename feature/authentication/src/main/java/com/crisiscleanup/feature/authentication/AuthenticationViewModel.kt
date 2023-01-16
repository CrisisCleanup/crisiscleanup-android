package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.jwt.JWT
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.feature.authentication.model.AuthenticationState
import com.crisiscleanup.feature.authentication.model.LoginInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val authApiClient: CrisisCleanupAuthApi,
    private val inputValidator: InputValidator,
    private val logger: AppLogger,
    private val appEnv: AppEnv,
    private val resProvider: AndroidResourceProvider,
) : ViewModel() {
    /**
     * Initial loading state of the view model
     *
     * Loading is complete when when the account data repository becomes accessible (the first time).
     */
    var isLoading = MutableStateFlow(true)
        private set

    private var isAuthenticating = MutableStateFlow(false)
    val isNotAuthenticating = isAuthenticating.map { !it }
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed()
        )

    val authenticationState: StateFlow<AuthenticationState> =
        accountDataRepository.accountData.map {
            if (isLoading.value) {
                resetLoginInputData()
                isLoading.value = false
            }

            AuthenticationState(
                accountData = it,
                hasAccessToken = it.accessToken.isNotEmpty(),
                // TODO Expiry must be dynamic not a snapshot
                isTokenExpired = it.tokenExpiry < Clock.System.now(),
            )
        }.stateIn(
            scope = viewModelScope,
            initialValue = AuthenticationState(),
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

    private suspend fun resetLoginInputData() {
        loginInputData.emailAddress = accountDataRepository.accountData.first().emailAddress
        loginInputData.password = ""
    }

    private fun clearErrors() {
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

        clearErrors()

        val emailAddress = loginInputData.emailAddress
        val password = loginInputData.password
        // TODO Test coverage
        if (!validateAuthInput(emailAddress, password)) {
            return
        }

        isAuthenticating.value = true
        viewModelScope.launch {
            try {
                val result = authApiClient.login(emailAddress, password)
                val hasError = (result.errors?.size ?: 0) > 0
                if (hasError) {
                    errorMessage.value = resProvider.getString(R.string.error_during_authentication)

                    val logErrorMessage = result.errors!![0].message?.get(0) ?: "Server error"
                    logger.logException(Exception(logErrorMessage))
                } else {
                    val accessToken = result.accessToken!!

                    val isDevBuild = appEnv.isDebuggable && accessToken == "access-token"
                    val expirySeconds: Long = if (isDevBuild) {
                        Clock.System.now().epochSeconds + 864000L
                    } else {
                        val jwt = JWT(accessToken)
                        Instant.fromEpochMilliseconds(jwt.expiresAt!!.time).epochSeconds
                    }

                    val claims = result.claims!!
                    val profilePicUri: String = claims.files?.firstOrNull {
                        it.fileTypeT == "fileTypes.user_profile_picture"
                    }?.url ?: ""
                    accountDataRepository.setAccount(
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

        clearErrors()

        isAuthenticating.value = true
        viewModelScope.launch {
            try {
                authApiClient.logout()

                loginInputData.apply {
                    emailAddress = ""
                    password = ""
                }
                accountDataRepository.clearAccount()
            } catch (e: Exception) {
                errorMessage.value = resProvider.getString(R.string.error_during_authentication)
                logger.logException(e)
            } finally {
                isAuthenticating.value = false
            }
        }
    }
}