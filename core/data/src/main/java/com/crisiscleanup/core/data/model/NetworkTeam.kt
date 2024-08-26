package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.model.data.closedWorkTypeStatuses
import com.crisiscleanup.core.model.data.statusFromLiteral
import com.crisiscleanup.core.network.model.NetworkTeam

fun NetworkTeam.asEntity(): TeamEntity {
    val workTypes = assignedWork ?: emptyList()
    val workTypeStatuses = workTypes.map { statusFromLiteral(it.status) }
    val completeCount = workTypeStatuses.filter { closedWorkTypeStatuses.contains(it) }.size
    return TeamEntity(
        id = 0,
        networkId = id,
        incidentId = incident,
        name = name,
        notes = notes ?: "",
        color = color,
        caseCount = assignedWork?.size ?: 0,
        completeCount = completeCount,
    )
}
