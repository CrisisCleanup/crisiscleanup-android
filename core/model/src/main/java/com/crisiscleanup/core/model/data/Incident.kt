package com.crisiscleanup.core.model.data

data class Incident(
    val id: Long,
    val name: String,
    val shortName: String,
    val locations: List<IncidentLocation>,
    val activePhoneNumber: String?,
)

data class IncidentLocation(
    val id: Long,
    val location: Long,
)