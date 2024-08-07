package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.CleanupTeam

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
    // TODO Equipments
)

fun PopulatedTeam.asExternalModel() = with(entity) {
    CleanupTeam(
        id = id,
        name = name,
        notes = notes,
        incidentId = incidentId,
        caseCount = caseCount,
        caseCompleteCount = completeCount,
        memberIds = memberIdRefs.map(TeamMemberCrossRef::contactId),
        members = members.map(PersonContactEntity::asExternalModel),
        equipment = emptyList(),
    )
}
