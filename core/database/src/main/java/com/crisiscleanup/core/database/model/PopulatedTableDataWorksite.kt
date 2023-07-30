package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class PopulatedTableDataWorksite(
    @Embedded
    val base: PopulatedWorksite,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val workTypeRequests: List<WorkTypeTransferRequestEntity>,
)

fun PopulatedTableDataWorksite.asExternalModel() = base.asExternalModel()
    .copy(workTypeRequests = workTypeRequests.map(WorkTypeTransferRequestEntity::asExternalModel))

