package com.crisiscleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.MainActivityUiState.Loading
import com.crisiscleanup.MainActivityUiState.Success
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val incidentsRepository: IncidentsRepository,
    val appHeaderUiState: AppHeaderUiState,
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = localAppPreferencesRepository.userData.map {
        Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000)
    )

    private var wasAuthenticated: Boolean? = null

    val authState: StateFlow<AuthState> = accountDataRepository.accountData.map {
        // TODO Remove this once full syncing pipeline is in place.
        //      Logging out should clear sync state so logging in syncs without requiring force.
        val isAuthenticated = it.accessToken.isNotEmpty()
        if (isAuthenticated) {
            val forceSync = wasAuthenticated == false
            syncIncidents(forceSync)
        }
        wasAuthenticated = isAuthenticated

        if (isAuthenticated) AuthState.Authenticated(it)
        else AuthState.NotAuthenticated
    }.stateIn(
        scope = viewModelScope,
        initialValue = AuthState.Loading,
        started = SharingStarted.WhileSubscribed()
    )

    private var syncJob: Job? = null

    init {
        incidentSelector.incidentId
            .onEach {
                syncIncidents(false)
            }
            .launchIn(viewModelScope)
    }

    private fun syncIncidents(force: Boolean) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            incidentsRepository.sync(force)
        }
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