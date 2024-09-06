package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.model.data.closedWorkTypeStatuses
import com.crisiscleanup.core.model.data.statusFromLiteral
import com.crisiscleanup.core.network.model.NetworkTeam
import com.crisiscleanup.core.network.model.NetworkTeamWork

fun NetworkTeam.asEntity(): TeamEntity {
    val workTypes = assignedWork ?: emptyList()
    val distinctWorksites = workTypes.map(NetworkTeamWork::worksite).toSet()
    val openWorksites = workTypes.mapNotNull {
        val status = statusFromLiteral(it.status)
        if (closedWorkTypeStatuses.contains(status)) {
            null
        } else {
            it.worksite
        }
    }
    val completeCount = distinctWorksites.size - openWorksites.size
    return TeamEntity(
        id = 0,
        networkId = id,
        incidentId = incident,
        name = name,
        notes = notes ?: "",
        color = color,
        caseCount = distinctWorksites.size,
        completeCount = completeCount,
    )
}
