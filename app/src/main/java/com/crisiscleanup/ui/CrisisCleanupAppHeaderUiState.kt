package com.crisiscleanup.ui

import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.appheader.AppHeaderState
import com.crisiscleanup.core.appheader.AppHeaderUiState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppHeaderUiState @Inject constructor() : AppHeaderUiState {
    override var appHeaderState = mutableStateOf(AppHeaderState.Default)
        private set

    override var title = mutableStateOf("")
        private set

    override fun setState(state: AppHeaderState) {
        appHeaderState.value = state
    }

    override fun setTitle(title: String) {
        this.title.value = title
    }
}