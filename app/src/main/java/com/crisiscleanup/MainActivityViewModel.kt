package com.crisiscleanup

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.commonassets.R
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppMetricsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.AppMetricsData
import com.crisiscleanup.core.model.data.AppOpenInstant
import com.crisiscleanup.core.model.data.BuildEndOfLife
import com.crisiscleanup.core.model.data.EarlybirdEndOfLifeFallback
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.MinSupportedAppVersion
import com.crisiscleanup.core.model.data.UserData
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    localAppPreferencesRepository: LocalAppPreferencesRepository,
    private val appMetricsRepository: LocalAppMetricsRepository,
    accountDataRepository: AccountDataRepository,
    incidentSelector: IncidentSelector,
    val appHeaderUiState: AppHeaderUiState,
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    appDataRepository: AppDataManagementRepository,
    accountDataRefresher: AccountDataRefresher,
    val translator: KeyResourceTranslator,
    private val syncPuller: SyncPuller,
    private val appVersionProvider: AppVersionProvider,
    private val appEnv: AppEnv,
    firebaseAnalytics: FirebaseAnalytics,
    authEventBus: AuthEventBus,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : ViewModel() {
    /**
     * Previous app open
     *
     * Sets only once every app session.
     */
    private val initialAppOpen = AtomicReference<AppOpenInstant>(null)

    val uiState = combine(
        localAppPreferencesRepository.userPreferences,
        appMetricsRepository.metrics.distinctUntilChanged(),
        ::Pair,
    )
        .map { (preferences, metrics) ->
            if (initialAppOpen.compareAndSet(null, metrics.appOpen)) {
                onAppOpen()
            }

            MainActivityUiState.Success(preferences, metrics)
        }.stateIn(
            scope = viewModelScope,
            initialValue = MainActivityUiState.Loading,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    /**
     * API account tokens need re-issuing
     */
    var isAccountExpired = mutableStateOf(false)
        private set

    val authState = accountDataRepository.accountData
        .map {
            isAccountExpired.value = !it.areTokensValid

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

    val disasterIconResId = incidentSelector.incident.map { getDisasterIcon(it.disaster) }
        .stateIn(
            scope = viewModelScope,
            initialValue = R.drawable.ic_disaster_other,
            started = SharingStarted.WhileSubscribed(),
        )

    private val isSyncingWorksitesFull = combine(
        incidentSelector.incidentId,
        worksitesRepository.syncWorksitesFullIncidentId,
    ) { incidentId, syncingIncidentId -> incidentId == syncingIncidentId }

    val showHeaderLoading = combine(
        incidentsRepository.isLoading,
        worksitesRepository.isLoading,
        isSyncingWorksitesFull,
    ) { b0, b1, b2 -> b0 || b1 || b2 }

    val buildEndOfLife: BuildEndOfLife?
        get() {
            if (appEnv.isEarlybird) {
                (uiState.value as? MainActivityUiState.Success)?.let {
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
                (uiState.value as? MainActivityUiState.Success)?.let {
                    return it.appMetrics.minSupportedAppVersion
                }
            }
            return null
        }

    val showPasswordReset = authEventBus.showResetPassword
    val showMagicLinkLogin = authEventBus.showMagicLinkLogin

    val isSwitchingToProduction: StateFlow<Boolean>
    val productionSwitchMessage: StateFlow<String>

    init {
        accountDataRepository.accountData
            .onEach {
                sync(false)
                syncPuller.appPullIncident(incidentSelector.incidentId.first())
                accountDataRefresher.updateMyOrganization(true)
            }
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

        localAppPreferencesRepository.userPreferences.onEach {
            firebaseAnalytics.setAnalyticsCollectionEnabled(it.allowAllAnalytics)
        }
            .launchIn(viewModelScope)

        syncPuller.appPullLanguage()
        syncPuller.appPullStatuses()

        val switchProductionApiManager = SwitchProductionApiManager(
            appMetricsRepository,
            appDataRepository,
            logger,
            viewModelScope,
        )
        uiState
            .filter { it is MainActivityUiState.Success }
            .onEach { switchProductionApiManager.switchToProduction() }
            .launchIn(viewModelScope)
        isSwitchingToProduction = switchProductionApiManager.isSwitchingToProduction
        productionSwitchMessage = switchProductionApiManager.productionSwitchMessage
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
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Success(
        val userData: UserData,
        val appMetrics: AppMetricsData,
    ) : MainActivityUiState
}

sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(val accountData: AccountData) : AuthState
    data object NotAuthenticated : AuthState
}
