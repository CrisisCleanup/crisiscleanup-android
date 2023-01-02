package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.CrisisCleanupPreferencesDataSource
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.ThemeBrand
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineFirstUserDataRepository @Inject constructor(
    private val crisisCleanupPreferencesDataSource: CrisisCleanupPreferencesDataSource
) : UserDataRepository {

    override val userData: Flow<UserData> =
        crisisCleanupPreferencesDataSource.userData

    override suspend fun setThemeBrand(themeBrand: ThemeBrand) =
        crisisCleanupPreferencesDataSource.setThemeBrand(themeBrand)

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) =
        crisisCleanupPreferencesDataSource.setDarkThemeConfig(darkThemeConfig)

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) =
        crisisCleanupPreferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)
}
