package com.crisiscleanup.feature.team

import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.commoncase.model.CoordinateBoundsDefault
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.feature.team.model.WorksiteQueryStateDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class TeamCasesQueryStateManager(
    incidentSelector: IncidentSelector,
    filterRepository: CasesFilterRepository,
    coroutineScope: CoroutineScope,
    mapChangeDebounceTimeout: Long = 100,
) {
    val isListView = MutableStateFlow(false)

    val mapZoom = MutableStateFlow(0f)

    val mapBounds = MutableStateFlow(CoordinateBoundsDefault)

    var worksiteQueryState = MutableStateFlow(WorksiteQueryStateDefault)

    init {
        incidentSelector.incident.onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(incidentId = it.id)
        }
            .launchIn(coroutineScope)

        isListView.onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(isListView = it)
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

        filterRepository.casesFiltersLocation.onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(filters = it.first)
        }
            .launchIn(coroutineScope)
    }
}
