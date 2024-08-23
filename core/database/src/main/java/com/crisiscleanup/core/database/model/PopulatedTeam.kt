package com.crisiscleanup.core.database.model

import android.graphics.Color
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.equipmentFromLiteral

data class PopulatedTeam(
    @Embedded
    val entity: TeamEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "team_id",
    )
    val memberIdRefs: List<TeamMemberCrossRef>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TeamMemberCrossRef::class,
            parentColumn = "team_id",
            entityColumn = "contact_id",
        ),
    )
    val members: List<PersonContactEntity>,

    // TODO Worksites/work types

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TeamEquipmentCrossRef::class,
            parentColumn = "team_id",
            entityColumn = "equipment_id",
        ),
    )
    val equipments: List<EquipmentEntity>,
)

fun PopulatedTeam.asExternalModel() = with(entity) {
    var colorInt = Color.TRANSPARENT
    try {
        colorInt = Color.parseColor(color)
    } catch (e: Exception) {
        // Keep default color
    }
    CleanupTeam(
        id = id,
        name = name,
        colorInt = colorInt,
        notes = notes,
        incidentId = incidentId,
        caseCount = caseCount,
        caseCompleteCount = completeCount,
        memberIds = memberIdRefs.map(TeamMemberCrossRef::contactId),
        members = members.map(PersonContactEntity::asExternalModel),
        equipment = equipments.map { equipmentFromLiteral(it.nameKey) },
    )
}
