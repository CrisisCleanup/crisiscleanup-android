package com.crisiscleanup

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    localAppPreferencesRepository: LocalAppPreferencesRepository,
    accountDataRepository: AccountDataRepository,
    incidentSelector: IncidentSelector,
    val appHeaderUiState: AppHeaderUiState,
    authEventBus: AuthEventBus,
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    private val syncPuller: SyncPuller,
    appEnv: AppEnv,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isDebuggable = appEnv.isDebuggable

    val uiState: StateFlow<MainActivityUiState> = localAppPreferencesRepository.userData.map {
        MainActivityUiState.Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = MainActivityUiState.Loading,
        started = SharingStarted.WhileSubscribed(5_000)
    )

    var isAccessTokenExpired = mutableStateOf(false)
        private set

    val authState: StateFlow<AuthState> = accountDataRepository.accountData.map {
        isAccessTokenExpired.value = it.isTokenExpired

        val hasAuthenticated = it.accessToken.isNotEmpty()

        if (hasAuthenticated) AuthState.Authenticated(it)
        else AuthState.NotAuthenticated
    }.stateIn(
        scope = viewModelScope,
        initialValue = AuthState.Loading,
        started = SharingStarted.WhileSubscribed()
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

    init {
        viewModelScope.launch {
            authEventBus.expiredTokens.collect { onExpiredToken() }
        }

        accountDataRepository.accountData
            .filter { !it.isTokenInvalid }
            .onEach {
                sync(false)
                syncPuller.appPullIncident(incidentSelector.incidentId.first())
            }
            .launchIn(viewModelScope)

        incidentSelector.incidentId
            .filter { it != EmptyIncident.id }
            .onEach {
                sync(true)
                syncPuller.appPullIncident(it)
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        syncPuller.appPullLanguage()
        syncPuller.appPullStatuses()
    }

    private fun sync(cancelOngoing: Boolean) {
        syncPuller.appPull(false, cancelOngoing)
    }

    private fun onExpiredToken() {
        isAccessTokenExpired.value = true
    }
}

sealed interface MainActivityUiState {
    object Loading : MainActivityUiState
    data class Success(val userData: UserData) : MainActivityUiState
}

sealed interface AuthState {
    object Loading : AuthState
    data class Authenticated(val accountData: AccountData) : AuthState
    object NotAuthenticated : AuthState
}