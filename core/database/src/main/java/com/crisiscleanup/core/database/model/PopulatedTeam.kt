package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.common.hexColorToIntColor
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

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val workTypes: List<TeamWorkEntity>,

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
    CleanupTeam(
        id = id,
        networkId = networkId,
        name = name,
        colorInt = color.hexColorToIntColor(),
        notes = notes,
        caseCount = caseCount,
        caseCompleteCount = completeCount,
        incidentId = incidentId,
        memberIds = memberIdRefs.map(TeamMemberCrossRef::contactId),
        members = members.asExternalModelSorted(),
        workTypeNetworkIds = workTypes.map(TeamWorkEntity::workTypeNetworkId),
        equipment = equipments.map { equipmentFromLiteral(it.nameKey) },
    )
}
