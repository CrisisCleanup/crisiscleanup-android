package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkIncidentsResult(
    val count: Int,
    val results: List<NetworkIncident>,
)

@Serializable
data class NetworkIncidentLocation(
    val id: Long,
    val location: Long,
)

@Serializable
data class NetworkIncident(

    // UPDATE NetworkIncidentTest in conjunction with changes here

    val id: Long,
    @SerialName("start_at")
    val startAt: Instant,
    val name: String,
    @SerialName("short_name")
    val shortName: String,
    val locations: List<NetworkIncidentLocation>,
    @SerialName("turn_on_release")
    val turnOnRelease: Boolean?,
    @SerialName("active_phone_number")
    val activePhoneNumber: String?,
    @SerialName("is_archived")
    val isArchived: Boolean?,
)