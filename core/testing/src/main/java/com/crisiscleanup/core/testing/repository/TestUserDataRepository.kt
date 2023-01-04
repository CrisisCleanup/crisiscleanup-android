package com.crisiscleanup.core.testing.repository

import com.crisiscleanup.core.data.repository.UserDataRepository
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

private val emptyUserData = UserData(
    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    shouldHideOnboarding = false
)

class TestUserDataRepository : UserDataRepository {
    /**
     * The backing hot flow for the list of followed topic ids for testing.
     */
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
}