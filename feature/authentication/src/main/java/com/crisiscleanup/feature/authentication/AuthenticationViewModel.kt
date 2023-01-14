package com.crisiscleanup.feature.authentication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.jwt.JWT
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.network.AccessTokenManager
import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.feature.authentication.model.AuthenticationState
import com.crisiscleanup.feature.authentication.model.LoginInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val authApiClient: CrisisCleanupAuthApi,
    private val logger: AppLogger,
    private val appEnv: AppEnv,
    private val accessTokenManager: AccessTokenManager,
) : ViewModel() {
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

    private suspend fun resetLoginInputData() {
        loginInputData.emailAddress = accountDataRepository.accountData.first().emailAddress
        loginInputData.password = ""
    }

    fun authenticateEmailPassword() {
        val emailAddress = loginInputData.emailAddress
        val password = loginInputData.password

        // TODO Validate data

        isAuthenticating.value = true
        viewModelScope.launch {
            try {
                accessTokenManager.accessToken = ""

                val result = authApiClient.login(emailAddress, password)
                val hasError = (result.errors?.size ?: 0) > 0
                if (hasError) {
                    // TODO Show message to user.
                    // TODO Only log exception if it is a system exception otherwise delete.
                    val errorMessage = result.errors!![0].message?.get(0) ?: "Server error"
                    logger.logException(Exception(errorMessage))
                } else {
                    val accessToken = result.accessToken!!

                    val isDevBuild = appEnv.isDebuggable && accessToken == "access-token"
                    val expirySeconds: Long = if (isDevBuild) {
                        Clock.System.now().epochSeconds + 8640000L
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
                    accessTokenManager.accessToken = ""
                }
            } catch (e: Exception) {
                // TODO show message to user
                logger.logException(e)
            } finally {
                isAuthenticating.value = false
            }
        }
    }

    fun logout() {
        isAuthenticating.value = true
        viewModelScope.launch {
            try {
                authApiClient.logout()
                Log.i("AUTH", "Logged out through API")

                accountDataRepository.clearAccount()
                accessTokenManager.accessToken = ""
            } catch (e: Exception) {
                // TODO show message to user
                logger.logException(e)
            } finally {
                isAuthenticating.value = false
            }
        }
    }
}