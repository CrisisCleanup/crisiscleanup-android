package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkTeamsResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val count: Int? = null,
    val results: List<NetworkTeam>? = null,
)

@Serializable
data class NetworkTeam(
    val id: Long,
    val name: String,
    val notes: String?,
    val incident: Long,
    val users: List<Long>?,
    @SerialName("assigned_work_types")
    val assignedWork: List<NetworkTeamWork>?,
    @SerialName("user_equipment_map")
    val userEquipments: List<NetworkUsersEquipment>,
    val color: String,
)

@Serializable
data class NetworkTeamWork(
    val id: Long,
    val status: String,
    @SerialName("work_type")
    val workType: String,
    val worksite: Long,
)

@Serializable
data class NetworkUsersEquipment(
    @SerialName("user_id")
    val userId: Long,
    @SerialName("equipment_ids")
    val equipmentIds: Set<Long>,
)

@Serializable
data class NetworkTeamResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val team: NetworkTeam? = null,
)
