package com.crisiscleanup.feature.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.model.data.AccountData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootAuthViewModel @Inject constructor(
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    @Dispatcher(CrisisCleanupDispatchers.IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val authState = accountDataRepository.accountData
        .map {
            if (it.areTokensValid) {
                AuthState.Authenticated(it)
            } else {
                AuthState.NotAuthenticated(it.hasAuthenticated)
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = AuthState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

    val hotlineIncidents = incidentsRepository.hotlineIncidents
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        viewModelScope.launch(ioDispatcher) {
            incidentsRepository.pullHotlineIncidents()
        }
    }
}

sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(val accountData: AccountData) : AuthState
    data class NotAuthenticated(val hasAuthenticated: Boolean) : AuthState
}
