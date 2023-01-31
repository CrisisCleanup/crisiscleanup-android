package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstLocalAppPreferencesRepository @Inject constructor(
    private val preferencesDataSource: LocalAppPreferencesDataSource
) : LocalAppPreferencesRepository {

    override val userData: Flow<UserData> = preferencesDataSource.userData

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) =
        preferencesDataSource.setDarkThemeConfig(darkThemeConfig)

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) =
        preferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)

    override suspend fun setSelectedIncident(id: Long) =
        preferencesDataSource.setSelectedIncident(id)
}
