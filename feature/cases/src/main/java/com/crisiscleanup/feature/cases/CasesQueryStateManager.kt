package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.feature.cases.model.CoordinateBoundsDefault
import com.crisiscleanup.feature.cases.model.WorksiteQueryStateDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class CasesQueryStateManager(
    incidentSelector: IncidentSelector,
    filterRepository: CasesFilterRepository,
    coroutineScope: CoroutineScope,
    mapChangeDebounceTimeout: Long = 50,
) {
    val isTableView = MutableStateFlow(false)

    val mapZoom = MutableStateFlow(0f)

    val mapBounds = MutableStateFlow(CoordinateBoundsDefault)

    val tableViewSort = MutableStateFlow(WorksiteSortBy.None)

    var worksiteQueryState = MutableStateFlow(WorksiteQueryStateDefault)

    init {
        incidentSelector.incident.onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(incidentId = it.id)
        }
            .launchIn(coroutineScope)

        isTableView.onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(isTableView = it)
        }
            .launchIn(coroutineScope)

        mapZoom
            .debounce(mapChangeDebounceTimeout)
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(zoom = it)
            }
            .launchIn(coroutineScope)

        mapBounds
            .debounce(mapChangeDebounceTimeout)
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(coordinateBounds = it)
            }
            .launchIn(coroutineScope)

        tableViewSort.onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(tableViewSort = it)
        }
            .launchIn(coroutineScope)

        filterRepository.casesFiltersLocation.onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(filters = it.first)
        }
            .launchIn(coroutineScope)
    }
}
