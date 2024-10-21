package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import com.crisiscleanup.core.model.data.WorksiteSortBy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesRepository @Inject constructor(
    private val preferencesDataSource: LocalAppPreferencesDataSource,
    accountEventBus: AccountEventBus,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LocalAppPreferencesRepository {

    override val userPreferences: Flow<UserData> = preferencesDataSource.userData

    @VisibleForTesting
    internal val observeJobs: List<Job>

    init {
        val logoutsJob = externalScope.launch(ioDispatcher) {
            accountEventBus.logouts.collect { onLogout() }
        }
        observeJobs = listOf(logoutsJob)
    }

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) =
        preferencesDataSource.setDarkThemeConfig(darkThemeConfig)

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) =
        preferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)

    override suspend fun setHideGettingStartedVideo(hide: Boolean) =
        preferencesDataSource.setHideGettingStartedVideo(hide)

    override suspend fun setMenuTutorialDone(isDone: Boolean) =
        preferencesDataSource.setMenuTutorialDone(isDone)

    override suspend fun setSelectedIncident(id: Long) =
        preferencesDataSource.setSelectedIncident(id)

    override suspend fun setLanguageKey(key: String) = preferencesDataSource.setLanguageKey(key)

    override suspend fun setTableViewSortBy(sortBy: WorksiteSortBy) =
        preferencesDataSource.setTableViewSortBy(sortBy)

    private suspend fun onLogout() {
        preferencesDataSource.reset()
    }

    override suspend fun setAnalytics(allowAll: Boolean) {
        preferencesDataSource.setAnalytics(allowAll)
    }
}
