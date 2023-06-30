package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.feature.cases.model.CoordinateBoundsDefault
import com.crisiscleanup.feature.cases.model.WorksiteQueryStateDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class CasesQueryStateManager(
    incidentSelector: IncidentSelector,
    coroutineScope: CoroutineScope,
    mapChangeDebounceTimeout: Long = 50,
) {
    val isTableView = MutableStateFlow(false)

    val mapZoom = MutableStateFlow(0f)

    val mapBounds = MutableStateFlow(CoordinateBoundsDefault)

    var worksiteQueryState = MutableStateFlow(WorksiteQueryStateDefault)

    init {
        incidentSelector.incident.onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(incidentId = it.id)
        }
            .launchIn(coroutineScope)

        isTableView.asStateFlow().onEach {
            worksiteQueryState.value = worksiteQueryState.value.copy(isTableView = it)
        }
            .launchIn(coroutineScope)

        mapZoom.asStateFlow()
            .debounce(mapChangeDebounceTimeout)
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(zoom = it)
            }
            .launchIn(coroutineScope)

        mapBounds.asStateFlow()
            .debounce(mapChangeDebounceTimeout)
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(coordinateBounds = it)
            }
            .launchIn(coroutineScope)
    }
}