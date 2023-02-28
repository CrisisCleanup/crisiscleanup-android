package com.crisiscleanup.ui

import com.crisiscleanup.core.appheader.AppHeaderState
import com.crisiscleanup.core.appheader.AppHeaderUiState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppHeaderUiState @Inject constructor() : AppHeaderUiState {
    private val titleStack = mutableListOf<String>()

    override var appHeaderState = MutableStateFlow(AppHeaderState.TopLevel)
        private set

    override var title = MutableStateFlow("")
        private set

    override fun setState(state: AppHeaderState) {
        appHeaderState.value = state
    }

    override fun setTitle(title: String) {
        this.title.value = title
    }

    override fun pushTitle(title: String) {
        if (this.title.value.isNotEmpty()) {
            titleStack.add(this.title.value)
        }
        setTitle(title)
    }

    override fun popTitle(popAll: Boolean) {
        if (titleStack.isNotEmpty()) {
            if (popAll) {
                setTitle(titleStack.first())
                titleStack.clear()
            } else {
                setTitle(titleStack.removeLast())
            }
        }
    }
}