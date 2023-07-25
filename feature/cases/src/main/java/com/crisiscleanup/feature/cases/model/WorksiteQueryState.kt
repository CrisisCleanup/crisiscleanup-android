package com.crisiscleanup.feature.cases.model

import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.google.android.gms.maps.model.LatLng

data class CoordinateBounds(
    val southWest: LatLng,
    val northEast: LatLng,
)

val CoordinateBoundsDefault = CoordinateBounds(
    LatLng(0.0, 0.0),
    LatLng(0.0, 0.0),
)

data class WorksiteQueryState(
    val incidentId: Long,
    val q: String,
    val zoom: Float,
    val coordinateBounds: CoordinateBounds,
    val isTableView: Boolean,
    val isZoomInteractive: Boolean,
    val tableViewSort: WorksiteSortBy,
    val filters: CasesFilter,
)

val WorksiteQueryStateDefault = WorksiteQueryState(
    incidentId = EmptyIncident.id,
    q = "",
    zoom = 0f,
    coordinateBounds = CoordinateBoundsDefault,
    isTableView = false,
    isZoomInteractive = false,
    tableViewSort = WorksiteSortBy.None,
    filters = CasesFilter(),
)
