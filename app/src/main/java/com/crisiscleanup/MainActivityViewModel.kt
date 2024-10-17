package com.crisiscleanup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.CrisisCleanupTutorialDirectors.Menu
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.TutorialDirector
import com.crisiscleanup.core.common.Tutorials
import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AccountUpdateRepository
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.LocalAppMetricsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.AppMetricsData
import com.crisiscleanup.core.model.data.AppOpenInstant
import com.crisiscleanup.core.model.data.BuildEndOfLife
import com.crisiscleanup.core.model.data.EarlybirdEndOfLifeFallback
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.MinSupportedAppVersion
import com.crisiscleanup.core.model.data.UserData
import com.crisiscleanup.core.ui.TutorialViewTracker
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    private val appMetricsRepository: LocalAppMetricsRepository,
    accountDataRepository: AccountDataRepository,
    incidentSelector: IncidentSelector,
    appDataRepository: AppDataManagementRepository,
    private val accountDataRefresher: AccountDataRefresher,
    private val accountUpdateRepository: AccountUpdateRepository,
    @Tutorials(Menu) private val menuTutorialDirector: TutorialDirector,
    val tutorialViewTracker: TutorialViewTracker,
    val translator: KeyResourceTranslator,
    private val syncPuller: SyncPuller,
    private val appVersionProvider: AppVersionProvider,
    appSettingsProvider: AppSettingsProvider,
    private val appEnv: AppEnv,
    firebaseAnalytics: FirebaseAnalytics,
    externalEventBus: ExternalEventBus,
    private val accountEventBus: AccountEventBus,
    private val networkMonitor: NetworkMonitor,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : ViewModel() {
    /**
     * Previous app open
     *
     * Sets only once every app session.
     */
    private val initialAppOpen = AtomicReference<AppOpenInstant>(null)

    val viewState = combine(
        appPreferencesRepository.userPreferences,
        appMetricsRepository.metrics.distinctUntilChanged(),
        ::Pair,
    )
        .map { (preferences, metrics) ->
            if (initialAppOpen.compareAndSet(null, metrics.appOpen)) {
                onAppOpen()
            }

            MainActivityViewState.Success(preferences, metrics)
        }.stateIn(
            scope = viewModelScope,
            initialValue = MainActivityViewState.Loading,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    /**
     * API account tokens need re-issuing
     */
    var isAccountExpired by mutableStateOf(false)
        private set

    val termsOfServiceUrl = appSettingsProvider.termsOfServiceUrl
    val privacyPolicyUrl = appSettingsProvider.privacyPolicyUrl
    var hasAcceptedTerms by mutableStateOf(false)
        private set
    var isAcceptingTerms by mutableStateOf(false)
    val isFetchingTermsAcceptance = MutableStateFlow(false)
    private val isUpdatingTermsAcceptance = MutableStateFlow(false)
    val isLoadingTermsAcceptance = combine(
        isFetchingTermsAcceptance,
        isUpdatingTermsAcceptance,
        ::Pair,
    )
        .map { (b0, b1) -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )
    var acceptTermsErrorMessage by mutableStateOf("")
        private set

    val authState = accountDataRepository.accountData
        .map {
            isAccountExpired = !it.areTokensValid
            hasAcceptedTerms = it.hasAcceptedTerms

            if (it.hasAuthenticated) {
                AuthState.Authenticated(it)
            } else {
                AuthState.NotAuthenticated
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = AuthState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

    val translationCount = translator.translationCount

    val buildEndOfLife: BuildEndOfLife?
        get() {
            if (appEnv.isEarlybird) {
                (viewState.value as? MainActivityViewState.Success)?.let {
                    var eol = it.appMetrics.earlybirdEndOfLife
                    if (!eol.isEndOfLife) {
                        eol = EarlybirdEndOfLifeFallback
                    }
                    if (eol.isEndOfLife) {
                        return eol
                    }
                }
            }
            return null
        }

    val supportedApp: MinSupportedAppVersion?
        get() {
            if (appEnv.isProduction) {
                (viewState.value as? MainActivityViewState.Success)?.let {
                    return it.appMetrics.minSupportedAppVersion
                }
            }
            return null
        }

    val showPasswordReset = externalEventBus.showResetPassword
    val showMagicLinkLogin = externalEventBus.showMagicLinkLogin
    val orgUserInvites = externalEventBus.orgUserInvites
    val orgPersistentInvites = externalEventBus.orgPersistentInvites
        .stateIn(
            scope = viewModelScope,
            initialValue = UserPersistentInvite(0, ""),
            started = SharingStarted.WhileSubscribed(),
        )
    var showInactiveOrganization by mutableStateOf(false)
        private set

    val menuTutorialStep = menuTutorialDirector.tutorialStep

    init {
        accountDataRepository.accountData
            .onEach {
                if (it.areTokensValid) {
                    sync(false)
                    syncPuller.appPullIncident(incidentSelector.incidentId.first())
                    accountDataRefresher.updateMyOrganization(true)
                    accountDataRefresher.updateApprovedIncidents()

                    logger.setAccountId(it.id.toString())
                } else {
                    if (!it.hasAcceptedTerms) {
                        accountEventBus.onLogout()
                    }
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        incidentSelector.incidentId
            .filter { it != EmptyIncident.id }
            .onEach {
                syncPuller.stopSyncPullWorksitesFull()
                sync(true)
                syncPuller.appPullIncident(it)
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        appPreferencesRepository.userPreferences.onEach {
            firebaseAnalytics.setAnalyticsCollectionEnabled(it.allowAllAnalytics)
        }
            .launchIn(viewModelScope)

        syncPuller.appPullLanguage()
        syncPuller.appPullStatuses()

        accountDataRepository.accountData
            .mapLatest { it.hasAcceptedTerms }
            .filter { !it }
            .throttleLatest(250)
            .onEach {
                isAcceptingTerms = false

                isFetchingTermsAcceptance.value = true
                try {
                    accountDataRefresher.updateAcceptedTerms()
                } finally {
                    isFetchingTermsAcceptance.value = false
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        accountEventBus.inactiveOrganizations
            .throttleLatest(5_000)
            .filter { it > 0 }
            .onEach {
                showInactiveOrganization = true
                logger.logDebug("Clearing app data from main")
                appDataRepository.clearAppData()
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)
    }

    private fun sync(cancelOngoing: Boolean) {
        syncPuller.appPull(false, cancelOngoing)
    }

    fun onAppOpen() {
        initialAppOpen.get()?.let {
            viewModelScope.launch {
                val previousOpen = appMetricsRepository.metrics.first().appOpen
                if (Clock.System.now() - previousOpen.date > 1.hours) {
                    appMetricsRepository.setAppOpen(appVersionProvider.versionCode)
                }
            }
        }
    }

    fun onRejectTerms() {
        acceptTermsErrorMessage = ""
        accountEventBus.onLogout()
    }

    fun onAcceptTerms() {
        acceptTermsErrorMessage = ""

        if (!isAcceptingTerms) {
            acceptTermsErrorMessage = translator("termsConditionsModal.must_check_box")
            return
        }

        if (isUpdatingTermsAcceptance.value) {
            return
        }
        isUpdatingTermsAcceptance.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                val isAccepted = accountUpdateRepository.acceptTerms()
                if (isAccepted) {
                    accountDataRefresher.updateAcceptedTerms()
                } else {
                    val errorMessage = if (networkMonitor.isOnline.first()) {
                        translator("termsConditionsModal.online_but_error")
                    } else {
                        translator("termsConditionsModal.offline_error")
                    }
                    acceptTermsErrorMessage = errorMessage
                }
            } finally {
                isUpdatingTermsAcceptance.value = false
            }
        }
    }

    private fun setMenuTutorialDone() {
        viewModelScope.launch(ioDispatcher) {
            appPreferencesRepository.setMenuTutorialDone(true)
        }
    }

    fun onMenuTutorialNext() {
        val hasNextStep = menuTutorialDirector.onNextStep()
        if (!hasNextStep) {
            setMenuTutorialDone()
        }
    }

    fun acknowledgeInactiveOrganization() {
        showInactiveOrganization = false
        accountEventBus.onLogout()
    }
}

sealed interface MainActivityViewState {
    data object Loading : MainActivityViewState
    data class Success(
        val userData: UserData,
        val appMetrics: AppMetricsData,
    ) : MainActivityViewState
}

sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(val accountData: AccountData) : AuthState
    data object NotAuthenticated : AuthState
}
