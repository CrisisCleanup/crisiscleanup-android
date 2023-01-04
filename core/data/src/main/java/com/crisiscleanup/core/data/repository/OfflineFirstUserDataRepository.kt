package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.CrisisCleanupPreferencesDataSource
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineFirstUserDataRepository @Inject constructor(
    private val preferencesDataSource: CrisisCleanupPreferencesDataSource
) : UserDataRepository {

    override val userData: Flow<UserData> = preferencesDataSource.userData

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) =
        preferencesDataSource.setDarkThemeConfig(darkThemeConfig)

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) =
        preferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)
}
