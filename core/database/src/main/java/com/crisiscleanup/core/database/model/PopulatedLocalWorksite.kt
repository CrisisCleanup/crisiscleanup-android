package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.model.data.LocalChange
import com.crisiscleanup.core.model.data.LocalWorksite
import com.crisiscleanup.core.model.data.Worksite

data class PopulatedLocalWorksite(
    @Embedded
    val entity: WorksiteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val workTypes: List<WorkTypeEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val root: WorksiteRootEntity,
)

fun PopulatedLocalWorksite.asExternalModel() = LocalWorksite(
    Worksite(

        // Be sure to copy changes from PopulatedWorksite.asExternalModel to here

        id = entity.id,
        address = entity.address,
        caseNumber = entity.caseNumber,
        city = entity.city,
        county = entity.county,
        createdAt = entity.createdAt,
        email = entity.email,
        favoriteId = entity.favoriteId,
        incident = entity.incidentId,
        keyWorkType = workTypes.find { it.workType == entity.keyWorkTypeType }?.asExternalModel(),
        latitude = entity.latitude,
        longitude = entity.longitude,
        name = entity.name,
        networkId = entity.networkId,
        phone1 = entity.phone1 ?: "",
        phone2 = entity.phone2 ?: "",
        plusCode = entity.plusCode,
        postalCode = entity.postalCode,
        reportedBy = entity.reportedBy,
        state = entity.state,
        svi = entity.svi,
        updatedAt = entity.updatedAt,
        what3words = entity.what3Words ?: "",
        workTypes = workTypes.map(WorkTypeEntity::asExternalModel),
    ),
    LocalChange(
        isLocalModified = root.isLocalModified,
        localModifiedAt = root.localModifiedAt,
        syncedAt = root.syncedAt,
    ),
)
