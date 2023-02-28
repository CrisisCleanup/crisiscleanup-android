package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.model.util.IterableStringSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkIncidentsResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkIncident>? = null,
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
    @SerialName("incident_type")
    val type: String,
    @SerialName("turn_on_release")
    val turnOnRelease: Boolean?,
    @Serializable(IterableStringSerializer::class)
    @SerialName("active_phone_number")
    val activePhoneNumber: List<String>?,
    @SerialName("is_archived")
    val isArchived: Boolean?,
)