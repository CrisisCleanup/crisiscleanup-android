package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkRequestRedeploy(
    val organization: Long,
    val incident: Long,
)

@Serializable
data class NetworkIncidentRedeployRequest(
    val id: Long,
    val organization: Long,
    val incident: Long,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("organization_name")
    val organizationName: String,
    @SerialName("incident_name")
    val incidentName: String,
)

@Serializable
data class NetworkRedeployRequestsResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkIncidentRedeployRequest>? = null,
)
