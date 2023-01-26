package com.crisiscleanup.ui

import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.appheader.AppHeaderBar
import com.crisiscleanup.core.appheader.AppHeaderState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppHeaderBar @Inject constructor() : AppHeaderBar {
    override var appHeaderState = mutableStateOf(AppHeaderState.Default)
        private set

    override fun setState(state: AppHeaderState) {
        appHeaderState.value = state
    }
}