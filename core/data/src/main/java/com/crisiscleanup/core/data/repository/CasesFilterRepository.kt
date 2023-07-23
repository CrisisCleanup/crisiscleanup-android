package com.crisiscleanup.core.data.repository

import android.util.LruCache
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.queryMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface CasesFilterRepository {
    val casesFilters: StateFlow<CasesFilter>
    val filtersCount: Flow<Int>
    val filterQueryParams: Flow<Map<String, Any?>>

    fun changeFilters(filters: CasesFilter)
}

@Singleton
class CrisisCleanupCasesFilterRepository @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    accountDataRepository: AccountDataRepository,
) : CasesFilterRepository {
    private val _casesFilters = MutableStateFlow(CasesFilter())
    override val casesFilters = _casesFilters

    override val filtersCount = casesFilters.map { it.changeCount }

    private val queryParamCache = LruCache<Pair<CasesFilter, Long>, Map<String, Any?>>(30)

    override val filterQueryParams = combine(
        accountDataRepository.accountData,
        casesFilters,
        ::Pair,
    )
        .filter { (accountData, _) -> accountData.org.id > 0 }
        .map { (accountData, filters) ->
            val orgId = accountData.org.id
            val cacheKey = Pair(filters, orgId)
            queryParamCache.get(cacheKey)?.let { cached ->
                return@map cached
            }

            if (filters.changeCount == 0) {
                queryParamCache.put(cacheKey, emptyMap())
                return@map emptyMap()
            }

            val queryParams = filters.queryMap(orgId)
            queryParamCache.put(cacheKey, queryParams)
            queryParams
        }

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