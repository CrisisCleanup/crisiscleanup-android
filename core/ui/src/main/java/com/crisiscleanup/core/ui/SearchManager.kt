package com.crisiscleanup.core.ui

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.StateFlow

interface SearchManager {
    // Not best practices but State allows for efficient recomposition while flow supports observability
    val searchQuery: State<String>
    val searchQueryFlow: StateFlow<String>

    fun updateSearchQuery(q: String)
}
