package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.model.data.closedWorkTypeStatuses
import com.crisiscleanup.core.model.data.statusFromLiteral
import com.crisiscleanup.core.network.model.NetworkTeam
import com.crisiscleanup.core.network.model.NetworkTeamWork

fun NetworkTeam.asEntity(): TeamEntity {
    val workTypes = assignedWork ?: emptyList()
    val distinctWorksites = workTypes.map(NetworkTeamWork::worksite).toSet()
    val closedWorkTypes = workTypes.filter {
        val status = statusFromLiteral(it.status)
        closedWorkTypeStatuses.contains(status)
    }
    val closedWorksites = closedWorkTypes.map(NetworkTeamWork::worksite).toSet()
    val completeCount = closedWorksites.size

    val workCount = workTypes.size
    val closedWorkCount = closedWorkTypes.size
    return TeamEntity(
        id = 0,
        networkId = id,
        incidentId = incident,
        name = name,
        notes = notes ?: "",
        color = color,
        caseCount = distinctWorksites.size,
        completeCount = completeCount,
        workCount = workCount,
        workCompleteCount = closedWorkCount,
    )
}
