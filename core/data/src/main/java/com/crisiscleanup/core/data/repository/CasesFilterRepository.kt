package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.CasesFiltersDataSource
import com.crisiscleanup.core.model.data.CasesFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface CasesFilterRepository {
    val casesFilters: CasesFilter
    val casesFiltersLocation: StateFlow<Pair<CasesFilter, Boolean>>
    val filtersCount: Flow<Int>

    fun changeFilters(filters: CasesFilter)
    fun updateWorkTypeFilters(workTypes: Collection<String>)
}

@Singleton
class CrisisCleanupCasesFilterRepository @Inject constructor(
    private val dataSource: CasesFiltersDataSource,
    permissionManager: PermissionManager,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CasesFilterRepository {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    override val casesFiltersLocation = combine(
        dataSource.casesFilters,
        permissionManager.hasLocationPermission,
        ::Pair,
    )
        .stateIn(
            scope = externalScope,
            initialValue = Pair(CasesFilter(), false),
            started = SharingStarted.WhileSubscribed(),
        )
    override val casesFilters: CasesFilter
        get() = casesFiltersLocation.value.first

    override val filtersCount = casesFiltersLocation.map { it.first.changeCount }

    // TODO Update or clear work type filters when incident changes

    override fun changeFilters(filters: CasesFilter) {
        externalScope.launch(ioDispatcher) {
            dataSource.updateFilters(filters)
        }
    }

    override fun updateWorkTypeFilters(workTypes: Collection<String>) {
        // TODO Update work types removing non-matching
    }
}
