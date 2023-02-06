package com.crisiscleanup.core.model.data

data class Incident(
    val id: Long,
    val name: String,
    val shortName: String,
    val locations: List<IncidentLocation>,
    val activePhoneNumbers: List<String>,
)

val EmptyIncident = Incident(-1, "", "", emptyList(), emptyList())

data class IncidentLocation(
    val id: Long,
    val location: Long,
)