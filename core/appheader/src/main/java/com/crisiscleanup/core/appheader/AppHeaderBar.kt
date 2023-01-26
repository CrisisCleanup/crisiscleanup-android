package com.crisiscleanup.core.appheader

import androidx.compose.runtime.State

interface AppHeaderBar {
    val appHeaderState: State<AppHeaderState>

    fun setState(state: AppHeaderState)
}

enum class AppHeaderState {
    None,

    // Incidents, comms, auth
    Default,
    SearchCases,
    TitleActions,
}