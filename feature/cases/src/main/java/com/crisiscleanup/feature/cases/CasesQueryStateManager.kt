package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.feature.cases.model.CoordinateBoundsDefault
import com.crisiscleanup.feature.cases.model.WorksiteQueryStateDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class CasesQueryStateManager(
    incidentSelector: IncidentSelector,
    filterRepository: CasesFilterRepository,
    preferencesRepository: LocalAppPreferencesRepository,
    coroutineScope: CoroutineScope,
    mapChangeDebounceTimeout: Long = 100,
) {
    val isTableView = MutableStateFlow(false)

    val mapZoom = MutableStateFlow(0f)

    val mapBounds = MutableStateFlow(CoordinateBoundsDefault)

    val tableViewSort = MutableStateFlow(WorksiteSortBy.None)

    var worksiteQueryState = MutableStateFlow(WorksiteQueryStateDefault)

    init {
        incidentSelector.incident
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(incidentId = it.id)
            }
            .launchIn(coroutineScope)

        isTableView
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(isTableView = it)
                preferencesRepository.setWorkScreenView(it)
            }
            .launchIn(coroutineScope)

        mapZoom
            .throttleLatest(mapChangeDebounceTimeout)
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(zoom = it)
            }
            .launchIn(coroutineScope)

        mapBounds
            .throttleLatest(mapChangeDebounceTimeout)
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(coordinateBounds = it)
            }
            .launchIn(coroutineScope)

        tableViewSort
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(tableViewSort = it)
            }
            .launchIn(coroutineScope)

        filterRepository.casesFiltersLocation
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(filters = it.first)
            }
            .launchIn(coroutineScope)

        coroutineScope.launch {
            val isTableViewCached =
                preferencesRepository.userPreferences.first().isWorkScreenTableView
            if (isTableViewCached != isTableView.value) {
                isTableView.value = isTableViewCached
            }
        }
    }
}
