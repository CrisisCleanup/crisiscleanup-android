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
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
    )
    val root: WorksiteRootEntity,
)

fun PopulatedWorksite.asExternalModel(): Worksite {
    val validWorkTypes = workTypes
    return with(entity) {
        Worksite(

            // Be sure to copy changes below to PopulatedLocalWorksite.asExternalModel

            id = id,
            networkId = networkId,
            address = address,
            autoContactFrequencyT = autoContactFrequencyT ?: "",
            caseNumber = caseNumber,
            city = city,
            county = county,
            createdAt = createdAt,
            email = email,
            favoriteId = favoriteId,
            incidentId = incidentId,
            keyWorkType = validWorkTypes.find { it.workType == keyWorkTypeType }
                ?.asExternalModel(),
            latitude = latitude,
            longitude = longitude,
            name = name,
            phone1 = phone1 ?: "",
            phone2 = phone2 ?: "",
            plusCode = plusCode,
            postalCode = postalCode,
            reportedBy = reportedBy,
            state = state,
            svi = svi,
            updatedAt = updatedAt,
            what3Words = what3Words ?: "",
            workTypes = validWorkTypes.map(WorkTypeEntity::asExternalModel),
            isAssignedToOrgMember = if (root.isLocalModified) isLocalFavorite else favoriteId != null,
        )
    }
}

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
    @ColumnInfo("favorite_id")
    val favoriteId: Long?,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val flags: List<WorksiteFlagEntity>,
)

fun PopulatedWorksiteMapVisual.asExternalModel() = WorksiteMapMark(
    id = id,
    latitude = latitude,
    longitude = longitude,
    statusClaim = WorkTypeStatusClaim.make(keyWorkTypeStatus, keyWorkTypeOrgClaim),
    workType = WorkTypeStatusClaim.getType(keyWorkTypeType),
    workTypeCount = workTypeCount,
    isFavorite = favoriteId != null,
    isHighPriority = flags.find { it.isHighPriority == true } != null,
)