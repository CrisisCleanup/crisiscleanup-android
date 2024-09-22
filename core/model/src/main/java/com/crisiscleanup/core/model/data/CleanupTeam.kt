package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant
import java.util.Collections.max
import java.util.Collections.min

data class TeamMetrics(
    val caseCount: Int,
    val caseCompleteCount: Int,
    val caseOpenCount: Int = caseCount - caseCompleteCount,
    val caseOverdueCount: Int = 0,
    val caseCompletePercentage: Int = if (caseCount > 0) {
        (caseCompleteCount.toFloat() / caseCount * 100).toInt()
    } else {
        0
    },
    private val workCount: Int,
    private val workCompleteCount: Int,
    val workCompletePercentage: Int = if (workCount > 0) {
        (workCompleteCount.toFloat() / workCount * 100).toInt()
    } else {
        0
    },
    val missingWorkCount: Int = 0,
    private val worksites: List<Worksite>,
    val firstActivityDate: Instant? = worksites
        .mapNotNull { it.createdAt?.epochSeconds }
        .let {
            if (it.isEmpty()) {
                null
            } else {
                Instant.fromEpochSeconds(min(it))
            }
        },
    val lastActivityDate: Instant? = worksites
        .mapNotNull { it.updatedAt?.epochSeconds }
        .let {
            if (it.isEmpty()) {
                null
            } else {
                Instant.fromEpochSeconds(max(it))
            }
        },
    val hasActivity: Boolean = firstActivityDate != null && lastActivityDate != null,
)

data class CleanupTeam(
    val id: Long,
    val networkId: Long,
    val name: String,
    val colorInt: Int,
    val notes: String,
    val incidentId: Long,
    val memberIds: Set<Long>,
    val members: List<PersonContact>,
    val equipment: List<CleanupEquipment> = emptyList(),
    val memberEquipment: List<MemberEquipment> = emptyList(),
    // TODO Work type IDs
    val worksites: List<Worksite> = emptyList(),
    val metrics: TeamMetrics,
)

val EmptyCleanupTeam = CleanupTeam(
    id = -1L,
    networkId = -1L,
    name = "",
    colorInt = 0,
    notes = "",
    incidentId = EmptyIncident.id,
    memberIds = emptySet(),
    members = emptyList(),
    metrics = TeamMetrics(
        caseCount = 0,
        caseCompleteCount = 0,
        workCount = 0,
        workCompleteCount = 0,
        worksites = emptyList(),
    ),
)

data class TeamWorksiteIds(
    val teamId: Long,
    val worksiteId: Long,
)
