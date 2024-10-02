package com.crisiscleanup.feature.team.model

import com.crisiscleanup.core.commoncase.model.CoordinateBounds
import com.crisiscleanup.core.commoncase.model.CoordinateBoundsDefault
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.EmptyIncident

internal data class TeamWorksiteQueryState(
    val incidentId: Long,
    val q: String,
    val zoom: Float,
    val coordinateBounds: CoordinateBounds,
    val isListView: Boolean,
    val isZoomInteractive: Boolean,
    val filters: CasesFilter,
)

internal val WorksiteQueryStateDefault = TeamWorksiteQueryState(
    incidentId = EmptyIncident.id,
    q = "",
    zoom = 0f,
    coordinateBounds = CoordinateBoundsDefault,
    isListView = false,
    isZoomInteractive = false,
    filters = CasesFilter(),
)
