package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.feature.cases.model.CoordinateBoundsDefault
import com.crisiscleanup.feature.cases.model.WorksiteQueryStateDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CasesQueryStateManager constructor(
    incidentSelector: IncidentSelector,
    private val appHeaderUiState: AppHeaderUiState,
    coroutineScope: CoroutineScope,
    searchQueryDebounceTimeout: Long = 150,
    mapChangeDebounceTimeout: Long = 50,
) {
    internal val casesSearchQueryFlow = MutableStateFlow("")

    internal val isTableView = MutableStateFlow(false)

    internal val mapZoom = MutableStateFlow(0f)

    internal val mapBounds = MutableStateFlow(CoordinateBoundsDefault)

    internal var worksiteQueryState = MutableStateFlow(WorksiteQueryStateDefault)

    init {
        incidentSelector.incident.onEach {
            appHeaderUiState.setTitle(it.name)

            worksiteQueryState.value = worksiteQueryState.value.copy(incidentId = it.id)
        }
            .launchIn(coroutineScope)

        casesSearchQueryFlow.asStateFlow()
            .debounce(searchQueryDebounceTimeout)
            .onEach {
                worksiteQueryState.value = worksiteQueryState.value.copy(q = it)
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