package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.feature.authentication.model.LoginInputData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class SaveCredentialsManager(
    private val coroutineScope: CoroutineScope,
    private val appPreferences: LocalAppPreferencesRepository,
    private val accountDataRepository: AccountDataRepository,
    private val authEventManager: AuthEventManager,
    private val logger: AppLogger,
) {
    private var isCredentialsRequested = false

    private var savedCredentials = LoginInputData()

    // TODO This flashes the first login after disabling the save credentials prompt.
    //      It should not. Investigate when time permits.
    private var _showSaveCredentialsAction = mutableStateOf(false)
    val showSaveCredentialsAction: State<Boolean> = _showSaveCredentialsAction

    val showDisableSaveCredentials = appPreferences.userData.map {
        it.saveCredentialsPromptCount > 2
    }
        .stateIn(
            scope = coroutineScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed()
        )

    fun resetState() {
        isCredentialsRequested = false
        savedCredentials = LoginInputData()
        _showSaveCredentialsAction.value = false
    }

    fun requestSavedCredentials() {
        if (isCredentialsRequested) {
            return
        }
        isCredentialsRequested = true
        coroutineScope.launch {
            authEventManager.onPasswordRequest()
        }
    }

    fun setSavedCredentials(credentials: LoginInputData) {
        savedCredentials = credentials
    }

    fun promptSaveCredentials(loginInputData: LoginInputData) {
        if (savedCredentials == loginInputData) {
            _showSaveCredentialsAction.value = false
        } else {
            coroutineScope.launch {
                appPreferences.incrementSaveCredentialsPrompt()
                val isAuthenticated = accountDataRepository.isAuthenticated.first()
                val isDisablePrompt = appPreferences.userData.first().disableSaveCredentialsPrompt
                _showSaveCredentialsAction.value = isAuthenticated && !isDisablePrompt
            }
        }
    }

    fun saveCredentials(credentials: LoginInputData) {
        if (credentials.emailAddress.isNotEmpty() &&
            credentials.password.isNotEmpty()
        ) {
            authEventManager.onSaveCredentials(credentials.emailAddress, credentials.password)
        }
    }

    fun setDisableSaveCredentials(disable: Boolean) {
        coroutineScope.launch {
            appPreferences.setDisableSaveCredentialsPrompt(disable)
        }
    }
}