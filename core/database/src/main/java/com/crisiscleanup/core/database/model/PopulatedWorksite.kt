package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.LatLng
import com.crisiscleanup.core.model.data.Worksite
import kotlinx.datetime.Instant

data class PopulatedWorksite(
    @Embedded
    val entity: WorksiteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorksiteWorkTypeCrossRef::class,
            parentColumn = "worksite_id",
            entityColumn = "work_type_id",
        )
    )
    val workTypes: List<WorkTypeEntity>
)

fun PopulatedWorksite.asExternalModel() = Worksite(
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
    location = LatLng(entity.latitude, entity.longitude),
    name = entity.name,
    phone1 = entity.phone1,
    phone2 = entity.phone2 ?: "",
    plusCode = entity.plusCode,
    postalCode = entity.postalCode,
    reportedBy = entity.reportedBy,
    state = entity.state,
    svi = entity.svi,
    updatedAt = entity.updatedAt,
    what3words = entity.what3Words ?: "",
    workTypes = workTypes.map(WorkTypeEntity::asExternalModel),
)

data class WorksiteLocalModifiedAt(
    val id: Long,
    @ColumnInfo("local_modified_at")
    val localModifiedAt: Instant,
)