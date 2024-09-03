package com.crisiscleanup.core.model.data

data class CleanupTeam(
    val id: Long,
    val networkId: Long,
    val name: String,
    val colorInt: Int,
    val notes: String,
    val caseCount: Int,
    private val caseCompleteCount: Int,
    val openCaseCount: Int = caseCount - caseCompleteCount,
    val caseCompletePercentage: Int =
        if (caseCount > 0) {
            (caseCompleteCount.toFloat() / caseCount * 100).toInt()
        } else {
            0
        },
    val incidentId: Long,
    val memberIds: List<Long>,
    val members: List<PersonContact>,
    val equipment: List<CleanupEquipment> = emptyList(),
    val memberEquipment: List<MemberEquipment> = emptyList(),
    private val workTypeIds: List<Long> = emptyList(),
    val worksites: List<Worksite> = emptyList(),
    val missingWorkTypeCount: Int = 0,
)

val EmptyCleanupTeam = CleanupTeam(
    id = -1L,
    networkId = -1L,
    name = "",
    colorInt = 0,
    notes = "",
    caseCount = 0,
    caseCompleteCount = 0,
    incidentId = EmptyIncident.id,
    memberIds = emptyList(),
    members = emptyList(),
    equipment = emptyList(),
)

data class PersonEquipment(
    val userId: Long,
    val equipment: CleanupEquipment,
    val count: Int = 1,
)

data class TeamWorksiteIds(
    val teamId: Long,
    val worksiteId: Long,
)
