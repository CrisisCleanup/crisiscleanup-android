package com.crisiscleanup.feature.cases.model

import com.crisiscleanup.core.commoncase.map.CasesQueryState
import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.commoncase.model.CoordinateBoundsDefault
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.WorksiteSortBy

internal data class WorksiteQueryState(
    override val incidentId: Long,
    override val zoom: Float,
    override val coordinateBounds: CoordinateBounds,
    val isTableView: Boolean,
    val tableViewSort: WorksiteSortBy,
    override val filters: CasesFilter,
) : CasesQueryState {
    override val isMapView = !isTableView
    override val teamCaseIds = emptySet<Long>()
}

internal val WorksiteQueryStateDefault = WorksiteQueryState(
    incidentId = EmptyIncident.id,
    zoom = 0f,
    coordinateBounds = CoordinateBoundsDefault,
    isTableView = false,
    tableViewSort = WorksiteSortBy.None,
    filters = CasesFilter(),
)
