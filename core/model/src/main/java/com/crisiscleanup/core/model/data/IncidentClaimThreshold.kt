package com.crisiscleanup.core.model.data

data class IncidentClaimThreshold(
    val incidentId: Long,
    val claimedCount: Int,
    val closedRatio: Float,
)
