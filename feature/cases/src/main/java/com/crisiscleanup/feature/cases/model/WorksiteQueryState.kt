package com.crisiscleanup.feature.cases.model

import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.commoncase.model.CoordinateBoundsDefault
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.WorksiteSortBy

internal data class WorksiteQueryState(
    val incidentId: Long,
    val q: String,
    val zoom: Float,
    val coordinateBounds: CoordinateBounds,
    val isTableView: Boolean,
    val isZoomInteractive: Boolean,
    val tableViewSort: WorksiteSortBy,
    val filters: CasesFilter,
)

internal val WorksiteQueryStateDefault = WorksiteQueryState(
    incidentId = EmptyIncident.id,
    q = "",
    zoom = 0f,
    coordinateBounds = CoordinateBoundsDefault,
    isTableView = false,
    isZoomInteractive = false,
    tableViewSort = WorksiteSortBy.None,
    filters = CasesFilter(),
)
