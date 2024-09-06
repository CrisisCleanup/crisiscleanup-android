package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.common.hexColorToIntColor
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.EquipmentData
import com.crisiscleanup.core.model.data.LocalChange
import com.crisiscleanup.core.model.data.LocalTeam
import com.crisiscleanup.core.model.data.MemberEquipment
import com.crisiscleanup.core.model.data.Worksite

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

    @Relation(
        parentColumn = "id",
        entityColumn = "network_id",
        associateBy = Junction(
            value = TeamWorkEntity::class,
            parentColumn = "id",
            entityColumn = "work_type_network_id",
        ),
    )
    val workTypes: List<WorkTypeEntity>,
)

fun PopulatedLocalTeam.asExternalModel(
    memberEquipment: List<MemberEquipment>,
    worksites: List<Worksite>,
    workIdLookup: Map<Long, Long>,
): LocalTeam {
    val workTypeIds = workTypes.map(WorkTypeEntity::id)
    val missingWorkTypeIds = workTypeIds.filter { !workIdLookup.contains(it) }
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
                members = members.asExternalModelSorted(),
                memberEquipment = memberEquipment,
                worksites = worksites,
                missingWorkTypeCount = missingWorkTypeIds.size,
            ),
            LocalChange(
                isLocalModified = root.isLocalModified,
                localModifiedAt = root.localModifiedAt,
                syncedAt = root.syncedAt,
            ),
        )
    }
}

data class PopulatedTeamMemberEquipment(
    val userId: Long,
    val userFirstName: String,
    val userLastName: String,
    val equipmentId: Long,
    val equipmentKey: String,
)

fun PopulatedTeamMemberEquipment.asExternalModel() = MemberEquipment(
    userId = userId,
    userName = "$userFirstName $userLastName".trim(),
    equipmentData = EquipmentData(
        id = equipmentId,
        nameKey = equipmentKey,
        listOrder = null,
        selectedCount = 0,
    ),
)
