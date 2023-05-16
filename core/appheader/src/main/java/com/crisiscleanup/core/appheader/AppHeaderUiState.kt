package com.crisiscleanup.core.appheader

import kotlinx.coroutines.flow.StateFlow

/**
 * State for app header UI
 */
interface AppHeaderUiState {
    val title: StateFlow<String>

    fun setTitle(title: String)
}
