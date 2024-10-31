package com.crisiscleanup.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appcomponent.AppTopBarDataProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.CrisisCleanupTutorialDirectors.Menu
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.TutorialDirector
import com.crisiscleanup.core.common.Tutorials
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupAccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.SyncLogRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.ui.TutorialViewTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    val incidentSelector: IncidentSelector,
    syncLogRepository: SyncLogRepository,
    private val accountDataRepository: AccountDataRepository,
    private val accountDataRefresher: AccountDataRefresher,
    private val appVersionProvider: AppVersionProvider,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    appSettingsProvider: AppSettingsProvider,
    private val appEnv: AppEnv,
    private val syncPuller: SyncPuller,
    @Tutorials(Menu) val menuTutorialDirector: TutorialDirector,
    val tutorialViewTracker: TutorialViewTracker,
    private val databaseVersionProvider: DatabaseVersionProvider,
    translator: KeyResourceTranslator,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isDebuggable = appEnv.isDebuggable
    val isNotProduction = appEnv.isNotProduction

    val termsOfServiceUrl = appSettingsProvider.termsOfServiceUrl
    val privacyPolicyUrl = appSettingsProvider.privacyPolicyUrl
    val gettingStartedVideoUrl = appSettingsProvider.gettingStartedVideoUrl

    val appTopBarDataProvider = AppTopBarDataProvider(
        "nav.menu",
        incidentsRepository,
        worksitesRepository,
        incidentSelector,
        translator,
        accountDataRepository,
        appPreferencesRepository,
        viewModelScope,
    )
    val incidentsData = appTopBarDataProvider.incidentsData
    val loadSelectIncidents = appTopBarDataProvider.loadSelectIncidents
    val isLoadingIncidents = incidentsRepository.isLoading

    val hotlineIncidents = incidentsRepository.hotlineIncidents
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val versionText: String
        get() {
            val version = appVersionProvider.version
            return "${version.second} (${version.first}) ${appEnv.apiEnvironment} Android"
        }

    val databaseVersionText: String
        get() = if (isNotProduction) "DB ${databaseVersionProvider.databaseVersion}" else ""

    val isSharingAnalytics = appPreferencesRepository.userPreferences.map {
        it.allowAllAnalytics
    }

    val isSharingLocation = appPreferencesRepository.userPreferences.map {
        it.shareLocationWithOrg
    }

    val menuItemVisibility = appPreferencesRepository.userPreferences
        .map {
            MenuItemVisibility(
                showOnboarding = !it.shouldHideOnboarding,
                showGettingStartedVideo = !it.hideGettingStartedVideo,
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = MenuItemVisibility(),
            started = SharingStarted.WhileSubscribed(),
        )

    val isMenuTutorialDone = appPreferencesRepository.userPreferences.map {
        it.isMenuTutorialDone
    }

    init {
        externalScope.launch(ioDispatcher) {
            syncLogRepository.trimOldLogs()

            accountDataRefresher.updateProfilePicture()
        }
    }

    suspend fun refreshIncidentsAsync() {
        syncPuller.pullIncidents()
    }

    fun refreshIncidents() {
        syncPuller.appPull(true, cancelOngoing = true)
    }

    fun shareAnalytics(share: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            appPreferencesRepository.setAnalytics(share)
        }
    }

    fun shareLocationWithOrg(share: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            appPreferencesRepository.setShareLocationWithOrg(share)
        }
    }

    fun simulateTokenExpired() {
        if (isDebuggable) {
            (accountDataRepository as CrisisCleanupAccountDataRepository).expireAccessToken()
        }
    }

    fun clearRefreshToken() {
        if (isDebuggable) {
            externalScope.launch {
                accountDataRepository.clearAccountTokens()
            }
        }
    }

    fun syncWorksitesFull() {
        if (isDebuggable) {
            syncPuller.scheduleSyncWorksitesFull()
        }
    }

    fun showGettingStartedVideo(show: Boolean) {
        val hide = !show
        viewModelScope.launch(ioDispatcher) {
            appPreferencesRepository.setHideGettingStartedVideo(hide)

            // TODO Move to hide onboarding method when implemented
            appPreferencesRepository.setShouldHideOnboarding(hide)
        }
    }

    fun setMenuTutorialDone(isDone: Boolean = true) {
        viewModelScope.launch(ioDispatcher) {
            appPreferencesRepository.setMenuTutorialDone(isDone)
        }
    }
}

data class MenuItemVisibility(
    val showOnboarding: Boolean = false,
    val showGettingStartedVideo: Boolean = false,
)
