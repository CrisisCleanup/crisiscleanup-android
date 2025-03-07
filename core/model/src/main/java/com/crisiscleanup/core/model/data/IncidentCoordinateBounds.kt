package com.crisiscleanup.core.model.data

data class IncidentCoordinateBounds(
    val incidentId: Long,
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
)

val IncidentCoordinateBoundsNone = IncidentCoordinateBounds(
    0,
    0.0,
    0.0,
    0.0,
    0.0,
)
