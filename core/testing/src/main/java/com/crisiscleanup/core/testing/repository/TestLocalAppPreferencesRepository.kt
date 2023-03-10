package com.crisiscleanup.core.testing.repository

import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull

private val emptyUserData = UserData(
    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    shouldHideOnboarding = false,
    syncAttempt = SyncAttempt(0, 0, 0),
    saveCredentialsPromptCount = 0,
    disableSaveCredentialsPrompt = false,
    selectedIncidentId = -1,
)

class TestLocalAppPreferencesRepository : LocalAppPreferencesRepository {
    private val _userData = MutableSharedFlow<UserData>(replay = 1, onBufferOverflow = DROP_OLDEST)

    private val currentUserData get() = _userData.replayCache.firstOrNull() ?: emptyUserData

    override val userData: Flow<UserData> = _userData.filterNotNull()

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        currentUserData.let { current ->
            _userData.tryEmit(current.copy(darkThemeConfig = darkThemeConfig))
        }
    }

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) {
        currentUserData.let { current ->
            _userData.tryEmit(current.copy(shouldHideOnboarding = shouldHideOnboarding))
        }
    }

    override suspend fun incrementSaveCredentialsPrompt() {
        currentUserData.let { current ->
            _userData.tryEmit(current.copy(saveCredentialsPromptCount = current.saveCredentialsPromptCount + 1))
        }
    }

    override suspend fun setDisableSaveCredentialsPrompt(disable: Boolean) {
        currentUserData.let { current ->
            _userData.tryEmit(current.copy(disableSaveCredentialsPrompt = disable))
        }
    }

    override suspend fun setSelectedIncident(id: Long) {
        currentUserData.let { current ->
            _userData.tryEmit(current.copy(selectedIncidentId = id))
        }
    }
}