package com.crisiscleanup.feature.team.model

import com.crisiscleanup.core.commoncase.map.CasesQueryState
import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.commoncase.model.CoordinateBoundsDefault
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.EmptyIncident

internal data class TeamWorksiteQueryState(
    override val incidentId: Long,
    override val zoom: Float,
    override val coordinateBounds: CoordinateBounds,
    val isListView: Boolean,
    override val filters: CasesFilter,
    override val teamCaseIds: Set<Long>,
) : CasesQueryState {
    override val isMapView = !isListView
}

internal val WorksiteQueryStateDefault = TeamWorksiteQueryState(
    incidentId = EmptyIncident.id,
    zoom = 0f,
    coordinateBounds = CoordinateBoundsDefault,
    isListView = false,
    filters = CasesFilter(),
    teamCaseIds = emptySet(),
)
