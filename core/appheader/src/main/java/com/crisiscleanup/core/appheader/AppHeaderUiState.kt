package com.crisiscleanup.core.appheader

import kotlinx.coroutines.flow.StateFlow

/**
 * State for app header UI
 */
interface AppHeaderUiState {
    val appHeaderState: StateFlow<AppHeaderState>

    val title: StateFlow<String>

    fun setState(state: AppHeaderState)
    fun setTitle(title: String)
}

enum class AppHeaderState {
    /** No title bar */
    None,

    /** Custom on left and account on right */
    TopLevel,
}