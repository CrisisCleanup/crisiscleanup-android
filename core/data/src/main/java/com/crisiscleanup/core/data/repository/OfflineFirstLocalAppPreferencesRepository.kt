package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.event.LogoutListener
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstLocalAppPreferencesRepository @Inject constructor(
    private val preferencesDataSource: LocalAppPreferencesDataSource,
    authEventManager: AuthEventManager,
) : LocalAppPreferencesRepository, LogoutListener {

    override val userData: Flow<UserData> = preferencesDataSource.userData

    init {
        authEventManager.addLogoutListener(this)
    }

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) =
        preferencesDataSource.setDarkThemeConfig(darkThemeConfig)

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) =
        preferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)

    override suspend fun setSelectedIncident(id: Long) =
        preferencesDataSource.setSelectedIncident(id)

    // LogoutListener

    override suspend fun onLogout() {
        preferencesDataSource.clearSyncData()
    }
}
