package com.crisiscleanup.ui

import com.crisiscleanup.core.appheader.AppHeaderState
import com.crisiscleanup.core.appheader.AppHeaderUiState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppHeaderUiState @Inject constructor() : AppHeaderUiState {
    override var appHeaderState = MutableStateFlow(AppHeaderState.Default)
        private set

    override var title = MutableStateFlow("")
        private set

    override fun setState(state: AppHeaderState) {
        appHeaderState.value = state
    }

    override fun setTitle(title: String) {
        this.title.value = title
    }
}