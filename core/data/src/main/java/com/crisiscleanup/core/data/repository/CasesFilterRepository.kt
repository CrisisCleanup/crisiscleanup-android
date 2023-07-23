package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface CasesFilterRepository {
    val casesFilters: StateFlow<CasesFilter>

    fun changeFilters(filters: CasesFilter)
}

val CasesFilterRepository.filterChangeCount: Int
    get() = casesFilters.value.changeCount

@Singleton
class CrisisCleanupCasesFilterRepository @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
) : CasesFilterRepository {
    private val _casesFilters = MutableStateFlow(CasesFilter())
    override val casesFilters = _casesFilters

    override fun changeFilters(filters: CasesFilter) {
        casesFilters.value = filters
    }

    fun getFilteredWorksiteIds(): List<Long>? {
        // TODO Get network worksites
        //      Update local
        //      Return IDs
        return null
    }
}