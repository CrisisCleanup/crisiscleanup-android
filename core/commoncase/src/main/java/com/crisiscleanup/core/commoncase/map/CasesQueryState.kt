package com.crisiscleanup.core.commoncase.map

import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.model.data.CasesFilter

interface CasesQueryState {
    val incidentId: Long
    val zoom: Float
    val coordinateBounds: CoordinateBounds
    val isMapView: Boolean
    val isNotMapView: Boolean
        get() = !isMapView
    val filters: CasesFilter
    val teamCaseIds: Set<Long>
}
