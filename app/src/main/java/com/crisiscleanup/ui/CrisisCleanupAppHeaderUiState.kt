package com.crisiscleanup.ui

import com.crisiscleanup.core.appheader.AppHeaderUiState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppHeaderUiState @Inject constructor() : AppHeaderUiState {
    override var title = MutableStateFlow("")
        private set

    override fun setTitle(title: String) {
        this.title.value = title
    }
}