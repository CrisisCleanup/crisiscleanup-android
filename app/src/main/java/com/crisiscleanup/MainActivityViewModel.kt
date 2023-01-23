package com.crisiscleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.MainActivityUiState.Loading
import com.crisiscleanup.MainActivityUiState.Success
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    localAppPreferencesRepository: LocalAppPreferencesRepository,
    accountDataRepository: AccountDataRepository,
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = localAppPreferencesRepository.userData.map {
        Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000)
    )

    val authState: StateFlow<AuthState> = accountDataRepository.accountData.map {
        if (it.accessToken.isNotEmpty()) AuthState.Authenticated(it)
        else AuthState.Other
    }.stateIn(
        scope = viewModelScope,
        initialValue = AuthState.Other,
        started = SharingStarted.WhileSubscribed()
    )
}

sealed interface MainActivityUiState {
    object Loading : MainActivityUiState
    data class Success(val userData: UserData) : MainActivityUiState
}

sealed interface AuthState {
    data class Authenticated(val accountData: AccountData) : AuthState

    // Loading or not authenticated
    object Other : AuthState
}