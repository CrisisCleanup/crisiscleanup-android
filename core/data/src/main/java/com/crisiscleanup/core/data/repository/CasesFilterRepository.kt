package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CasesFilterRepository @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
) {
    val casesFilter = MutableStateFlow(CasesFilter())

    val filterChangeCount get() = casesFilter.value.changeCount

    fun changeFilter(changed: CasesFilter) {
        casesFilter.value = changed
    }

    fun getFilteredWorksiteIds(): List<Long>? {
        // TODO Get network worksites
        //      Update local
        //      Return IDs
        return null
    }
}