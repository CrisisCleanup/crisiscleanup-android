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
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface CasesFilterRepository {
    val casesFilters: CasesFilter
    val casesFiltersLocation: StateFlow<Triple<CasesFilter, Boolean, Long>>
    val filtersCount: Flow<Int>

    suspend fun changeFilters(filters: CasesFilter)
    fun updateWorkTypeFilters(workTypes: Collection<String>)
    fun reapplyFilters()
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

    private val applyFilterTimestamp = MutableStateFlow(0L)

    override val casesFiltersLocation = combine(
        dataSource.casesFilters,
        permissionManager.hasLocationPermission,
        applyFilterTimestamp,
        ::Triple,
    )
        .stateIn(
            scope = externalScope,
            initialValue = Triple(CasesFilter(), false, 0L),
            started = SharingStarted.WhileSubscribed(),
        )
    override val casesFilters: CasesFilter
        get() = casesFiltersLocation.value.first

    override val filtersCount = casesFiltersLocation.map { it.first.changeCount }

    // TODO Update or clear work type filters when incident changes

    override suspend fun changeFilters(filters: CasesFilter) = withContext(ioDispatcher) {
        dataSource.updateFilters(filters)
    }

    override fun updateWorkTypeFilters(workTypes: Collection<String>) {
        // TODO Update work types removing non-matching
    }

    override fun reapplyFilters() {
        if (casesFilters.changeCount > 0) {
            applyFilterTimestamp.value = Clock.System.now().epochSeconds
        }
    }
}
