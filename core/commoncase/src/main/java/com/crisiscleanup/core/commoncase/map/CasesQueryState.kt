package com.crisiscleanup.core.commoncase.map

import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.model.data.CasesFilter

interface CasesQueryState {
    val incidentId: Long
    val q: String
    val zoom: Float
    val coordinateBounds: CoordinateBounds
    val isMapView: Boolean
    val isNotMapView: Boolean
        get() = !isMapView
    val isZoomInteractive: Boolean
    val filters: CasesFilter
}
