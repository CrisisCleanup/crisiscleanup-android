package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstLocalAppPreferencesRepository @Inject constructor(
    private val preferencesDataSource: LocalAppPreferencesDataSource,
    authEventBus: AuthEventBus,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LocalAppPreferencesRepository {

    override val userPreferences: Flow<UserData> = preferencesDataSource.userData

    @VisibleForTesting
    internal val observeJobs: List<Job>

    init {
        val logoutsJob = externalScope.launch(ioDispatcher) {
            authEventBus.logouts.collect { onLogout() }
        }
        observeJobs = listOf(logoutsJob)
    }

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) =
        preferencesDataSource.setDarkThemeConfig(darkThemeConfig)

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) =
        preferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)

    override suspend fun incrementSaveCredentialsPrompt() {
        preferencesDataSource.incrementSaveCredentialsPrompt()
    }

    override suspend fun setDisableSaveCredentialsPrompt(disable: Boolean) {
        preferencesDataSource.setDisableSaveCredentialsPrompt(disable)
    }

    override suspend fun setSelectedIncident(id: Long) =
        preferencesDataSource.setSelectedIncident(id)

    override suspend fun setLanguageKey(key: String) = preferencesDataSource.setLanguageKey(key)

    private suspend fun onLogout() {
        preferencesDataSource.clearSyncData()
    }
}
