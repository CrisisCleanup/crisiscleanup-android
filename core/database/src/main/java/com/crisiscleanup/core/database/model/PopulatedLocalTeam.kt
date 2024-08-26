package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.common.hexColorToIntColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.LocalChange
import com.crisiscleanup.core.model.data.LocalTeam
import com.crisiscleanup.core.model.data.equipmentFromLiteral

data class PopulatedLocalTeam(
    @Embedded
    val entity: TeamEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val root: TeamRootEntity,
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

    // TODO Look up latest equipment of members?
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

fun PopulatedLocalTeam.asExternalModel(): LocalTeam {
    return with(entity) {
        LocalTeam(
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
                members = members.map(PersonContactEntity::asExternalModel),
                equipment = equipments.map { equipmentFromLiteral(it.nameKey) },
            ),
            LocalChange(
                isLocalModified = root.isLocalModified,
                localModifiedAt = root.localModifiedAt,
                syncedAt = root.syncedAt,
            ),
        )
    }
}
