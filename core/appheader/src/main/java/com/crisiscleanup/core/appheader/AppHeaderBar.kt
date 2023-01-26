package com.crisiscleanup.core.appheader

import androidx.compose.runtime.State

interface AppHeaderBar {
    val appHeaderState: State<AppHeaderState>

    val title: State<String>

    fun setState(state: AppHeaderState)
    fun setTitle(title: String)
}

enum class AppHeaderState {
    None,

    // Incidents, comms, auth
    Default,
    SearchCases,
    TitleActions,
}