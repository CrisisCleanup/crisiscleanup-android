package com.crisiscleanup.ui

import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.ui.SearchManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AppSearchManager @Inject constructor() : SearchManager {
    override var searchQuery = mutableStateOf("")
        private set

    private val _searchQueryFlow = MutableStateFlow("")
    override val searchQueryFlow: StateFlow<String> = _searchQueryFlow

    override fun updateSearchQuery(q: String) {
        searchQuery.value = q
        _searchQueryFlow.value = q
    }
}