package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.event.PasswordRequestCode
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
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
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val authApiClient: CrisisCleanupAuthApi,
    private val inputValidator: InputValidator,
    private val accessTokenDecoder: AccessTokenDecoder,
    private val authEventBus: AuthEventBus,
    appPreferences: LocalAppPreferencesRepository,
    private val resProvider: AndroidResourceProvider,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
) : ViewModel() {
    private var isAuthenticating = MutableStateFlow(false)
    val isNotAuthenticating = isAuthenticating.map(Boolean::not)
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed()
        )

    private val saveCredentialsManager = SaveCredentialsManager(
        viewModelScope,
        appPreferences,
        accountDataRepository,
        authEventBus,
        logger,
    )

    // This may fail when a user cancels too many times.
    // Wait 24 hours or enter the code at https://developer.android.com/training/sign-in/passkeys#troubleshoot
    val showSaveCredentialsAction = saveCredentialsManager.showSaveCredentialsAction
    val showDisableSaveCredentials = saveCredentialsManager.showDisableSaveCredentials

    val uiState: StateFlow<AuthenticateScreenUiState> =
        accountDataRepository.accountData.map {
            if (loginInputData.emailAddress.isEmpty() &&
                it.emailAddress.isNotEmpty()
            ) {
                loginInputData.emailAddress = it.emailAddress
                loginInputData.password = ""

                saveCredentialsManager.resetState()
            }

            AuthenticateScreenUiState.Ready(
                authenticationState = AuthenticationState(accountData = it),
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

    init {
        viewModelScope.launch {
            authEventBus.passwordCredentialResults.collect {
                it.apply {
                    onPasswordCredentialsResult(emailAddress, password, resultCode)
                }
            }
        }
    }

    // TODO Test (if not included in other tests)
    fun onCloseScreen() {
        loginInputData.password = ""
        errorMessage.value = ""
        isInvalidEmail.value = false
        isInvalidEmail.value = false
        saveCredentialsManager.resetState()
    }

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
                val hasError = result.errors?.isNotEmpty() == true
                if (hasError) {
                    errorMessage.value = resProvider.getString(R.string.error_during_authentication)

                    val logErrorMessage = result.errors?.condenseMessages ?: "Server error"
                    logger.logException(Exception(logErrorMessage))
                } else {
                    val accessToken = result.accessToken!!

                    val expirySeconds =
                        accessTokenDecoder.decode(accessToken).expiresAt.epochSeconds

                    val claims = result.claims!!
                    val profilePicUri =
                        claims.files?.firstOrNull(NetworkFile::isProfilePicture)?.largeThumbnailUrl
                            ?: ""

                    // TODO Test coverage
                    val organization = result.organizations
                    val orgData = if (
                        organization?.isActive == true &&
                        organization.id >= 0 &&
                        organization.name.isNotEmpty()
                    ) {
                        OrgData(
                            organization.id,
                            organization.name,
                        )
                    } else {
                        emptyOrgData
                    }

                    accountDataRepository.setAccount(
                        id = claims.id,
                        accessToken = accessToken,
                        email = claims.email,
                        firstName = claims.firstName,
                        lastName = claims.lastName,
                        expirySeconds = expirySeconds,
                        profilePictureUri = profilePicUri,
                        org = orgData,
                    )

                    // TODO Update tests
                    saveCredentialsManager.promptSaveCredentials(loginInputData)
                }
            } catch (e: Exception) {
                var isInvalidCredentials = false
                if (e is CrisisCleanupNetworkException) {
                    isInvalidCredentials = e.statusCode == 400
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

        loginInputData.apply {
            emailAddress = ""
            password = ""
        }
        authEventBus.onLogout()
    }

    // TODO Test save credentials related below
    fun onInputFocus() {
        if (loginInputData.password.isEmpty()) {
            saveCredentialsManager.requestSavedCredentials()
        }
    }

    fun saveCredentials() = saveCredentialsManager.saveCredentials(loginInputData)

    fun setDisableSaveCredentials(disable: Boolean) =
        saveCredentialsManager.setDisableSaveCredentials(disable)

    private fun onPasswordCredentialsResult(
        emailAddress: String,
        password: String,
        resultCode: PasswordRequestCode
    ) {
        if (resultCode == PasswordRequestCode.Success && emailAddress.isNotEmpty()) {
            saveCredentialsManager.setSavedCredentials(LoginInputData(emailAddress, password))
            loginInputData.apply {
                this.emailAddress = emailAddress
                this.password = password
            }
            authenticateEmailPassword()
        }
    }
}

sealed interface AuthenticateScreenUiState {
    object Loading : AuthenticateScreenUiState
    data class Ready(val authenticationState: AuthenticationState) : AuthenticateScreenUiState
}