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
    @SerialName("object_id")
    val objectId: Long,
    @SerialName("created_at")
    val createdAt: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetworkIncidentLocation

        if (id != other.id) return false
        if (location != other.location) return false
        if (objectId != other.objectId) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Serializable
data class NetworkIncident(
    // UPDATE NetworkAuthTest in conjunction with changes here
    val id: Long,
    val name: String,
    @SerialName("short_name")
    val shortName: String,
    val locations: List<NetworkIncidentLocation>,
    @SerialName("turn_on_release")
    val turnOnRelease: Boolean,
    @SerialName("active_phone_number")
    val activePhoneNumber: String?,
) {
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
