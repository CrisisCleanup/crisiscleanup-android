package com.crisiscleanup

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.event.ExpiredTokenListener
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
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    localAppPreferencesRepository: LocalAppPreferencesRepository,
    accountDataRepository: AccountDataRepository,
    incidentSelector: IncidentSelector,
    val appHeaderUiState: AppHeaderUiState,
    private val authEventManager: AuthEventManager,
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    private val syncPuller: SyncPuller,
    appEnv: AppEnv,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel(), ExpiredTokenListener {
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

        val isAuthenticated = it.accessToken.isNotEmpty()

        if (isAuthenticated) AuthState.Authenticated(it)
        else AuthState.NotAuthenticated
    }.stateIn(
        scope = viewModelScope,
        initialValue = AuthState.Loading,
        started = SharingStarted.WhileSubscribed()
    )

    val showHeaderLoading = combine(
        incidentsRepository.isLoading,
        worksitesRepository.isLoading,
    ) {
            incidentsLoading,
            worksitesLoading,
        ->
        incidentsLoading ||
                worksitesLoading
    }

    private var expiredTokenListenerId = -1

    init {
        expiredTokenListenerId = authEventManager.addExpiredTokenListener(this)

        authState
            .filter { it is AuthState.Authenticated }
            .onEach { sync(false) }
            .launchIn(viewModelScope)

        incidentSelector.incidentId
            .filter { it != EmptyIncident.id }
            .onEach {
                val worksitesCount = worksitesRepository.getWorksitesCount(it)
                sync(worksitesCount == 0)
                syncPuller.appPullIncident(it)
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        syncPuller.appPullLanguage()
    }

    override fun onCleared() {
        authEventManager.removeExpiredTokenListener(expiredTokenListenerId)
    }

    private fun sync(cancelOngoing: Boolean) {
        syncPuller.appPull(false, cancelOngoing)
    }

    // ExpiredTokenListener

    override fun onExpiredToken() {
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