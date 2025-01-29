package com.crisiscleanup.feature.cases.model

import com.crisiscleanup.core.model.data.IncidentCoordinateBounds
import com.google.android.gms.maps.model.LatLngBounds

internal fun LatLngBounds.asIncidentCoordinateBounds(incidentId: Long) = IncidentCoordinateBounds(
    incidentId,
    south = southwest.latitude,
    west = southwest.longitude,
    north = northeast.latitude,
    east = northeast.longitude,
)
