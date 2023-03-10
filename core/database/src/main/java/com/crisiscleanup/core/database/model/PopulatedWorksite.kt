package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import kotlinx.datetime.Instant

data class PopulatedWorksite(
    @Embedded
    val entity: WorksiteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val workTypes: List<WorkTypeEntity>,
)

fun PopulatedWorksite.asExternalModel() = Worksite(

    // Be sure to copy changes below to PopulatedLocalWorksite.asExternalModel

    id = entity.id,
    networkId = entity.networkId,
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
)

data class WorksiteLocalModifiedAt(
    @ColumnInfo("id")
    val id: Long,
    @ColumnInfo("network_id")
    val networkId: Long,
    @ColumnInfo("local_modified_at")
    val localModifiedAt: Instant,
    @ColumnInfo("is_local_modified")
    val isLocallyModified: Boolean,
)

data class PopulatedWorksiteMapVisual(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo("key_work_type_type")
    val keyWorkTypeType: String,
    @ColumnInfo("key_work_type_org")
    val keyWorkTypeOrgClaim: Long?,
    @ColumnInfo("key_work_type_status")
    val keyWorkTypeStatus: String,
    @ColumnInfo("work_type_count")
    val workTypeCount: Int,
)

fun PopulatedWorksiteMapVisual.asExternalModel() = WorksiteMapMark(
    id = id,
    latitude = latitude,
    longitude = longitude,
    statusClaim = WorkTypeStatusClaim.make(keyWorkTypeStatus, keyWorkTypeOrgClaim),
    workType = WorkTypeStatusClaim.getType(keyWorkTypeType),
    workTypeCount = workTypeCount,
)