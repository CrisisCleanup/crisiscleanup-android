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
) {
    // For tests. equals can compare on id only.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetworkIncidentLocation

        if (id != other.id) return false
        if (location != other.location) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

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
    val turnOnRelease: Boolean,
    @SerialName("active_phone_number")
    val activePhoneNumber: String?,
) {
    // For tests. equals can compare on id only.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetworkIncident

        if (id != other.id) return false
        if (name != other.name) return false
        if (shortName != other.shortName) return false
        if (locations != other.locations) return false
        if (turnOnRelease != other.turnOnRelease) return false
        if (activePhoneNumber != other.activePhoneNumber) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
