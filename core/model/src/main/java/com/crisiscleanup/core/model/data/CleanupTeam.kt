package com.crisiscleanup.core.model.data

data class CleanupTeam(
    val id: Long,
    val name: String,
    val colorInt: Int,
    val notes: String,
    val caseCount: Int,
    private val caseCompleteCount: Int,
    val caseCompletePercentage: Int =
        if (caseCount > 0) {
            (caseCompleteCount.toFloat() / caseCount * 100).toInt()
        } else {
            0
        },
    val incidentId: Long,
    val memberIds: List<Long>,
    val members: List<PersonContact>,
    val equipment: List<CleanupEquipment>,
)

data class PersonEquipment(
    val userId: Long,
    val equipment: CleanupEquipment,
    val count: Int = 1,
)
