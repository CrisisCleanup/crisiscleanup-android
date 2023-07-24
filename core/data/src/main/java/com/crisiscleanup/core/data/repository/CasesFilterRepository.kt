package com.crisiscleanup.core.data.repository

import android.util.LruCache
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.CasesFiltersDataSource
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.queryMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface CasesFilterRepository {
    val casesFilters: StateFlow<CasesFilter>
    val filtersCount: Flow<Int>
    val filterQuery: Flow<Pair<CasesFilter, Map<String, Any>>>

    fun changeFilters(filters: CasesFilter)
    fun updateWorkTypeFilters(workTypes: Collection<String>)
}

@Singleton
class CrisisCleanupCasesFilterRepository @Inject constructor(
    private val dataSource: CasesFiltersDataSource,
    private val networkMonitor: NetworkMonitor,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    accountDataRepository: AccountDataRepository,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CasesFilterRepository {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _casesFilters = MutableStateFlow(CasesFilter())
    override val casesFilters = _casesFilters

    override val filtersCount = casesFilters.map { it.changeCount }

    private val queryParamCache = LruCache<Pair<CasesFilter, Long>, Map<String, Any>>(30)

    override val filterQuery = combine(
        accountDataRepository.accountData,
        casesFilters,
        ::Pair,
    )
        .filter { (accountData, _) -> accountData.org.id > 0 }
        .map { (accountData, filters) ->
            val orgId = accountData.org.id
            val cacheKey = Pair(filters, orgId)
            queryParamCache.get(cacheKey)?.let { cached ->
                return@map Pair(filters, cached)
            }

            if (filters.changeCount == 0) {
                queryParamCache.put(cacheKey, emptyMap())
                return@map Pair(filters, emptyMap())
            }

            val queryParams = filters.queryMap(orgId)
            queryParamCache.put(cacheKey, queryParams)
            Pair(filters, queryParams)
        }

    init {
        externalScope.launch(ioDispatcher) {
            dataSource.casesFilters
                .onEach {
                    _casesFilters.value = it
                }
                .collect()
        }
    }

    override fun changeFilters(filters: CasesFilter) {
        externalScope.launch(ioDispatcher) {
            dataSource.updateFilters(filters)
        }
    }

    override fun updateWorkTypeFilters(workTypes: Collection<String>) {
        // TODO Update work types removing non-matching
    }

    fun getFilteredWorksiteIds(): List<Long>? {
        // TODO Get network worksites
        //      Update local
        //      Return IDs
        return null
    }
}