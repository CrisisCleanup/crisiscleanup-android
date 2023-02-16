package com.crisiscleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.MainActivityUiState.Loading
import com.crisiscleanup.MainActivityUiState.Success
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.Syncer
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    localAppPreferencesRepository: LocalAppPreferencesRepository,
    accountDataRepository: AccountDataRepository,
    incidentSelector: IncidentSelector,
    val appHeaderUiState: AppHeaderUiState,
    incidentsRepository: IncidentsRepository,
    worksitesRepository: WorksitesRepository,
    private val syncer: Syncer,
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = localAppPreferencesRepository.userData.map {
        Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000)
    )

    val authState: StateFlow<AuthState> = accountDataRepository.accountData.map {
        syncIncidents()
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

    init {
        incidentSelector.incidentId
            .onEach {
                syncIncidents()
            }
            .launchIn(viewModelScope)
    }

    private fun syncIncidents() {
        syncer.sync()
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