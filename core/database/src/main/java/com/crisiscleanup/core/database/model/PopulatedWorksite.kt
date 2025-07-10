package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.core.model.data.WorksiteMapMark
import kotlinx.datetime.Instant

/**
 * For read-only purposes with minimal data
 *
 * Use [PopulatedLocalWorksite] for copying, modifying, and mutating
 */
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
) {
    internal val isFavorite: Boolean
        get() {
            return if (root.isLocalModified) entity.isLocalFavorite else entity.favoriteId != null
        }
}

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

    // Filter fields
    @ColumnInfo("created_at")
    val createdAt: Instant? = null,
    @ColumnInfo("is_local_favorite")
    val isLocalFavorite: Boolean = false,
    @ColumnInfo("reported_by")
    val reportedBy: Long?,
    val svi: Float?,
    @ColumnInfo("updated_at")
    val updatedAt: Instant,
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
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val formData: List<WorksiteFormDataEntity>,

    // Has photo
    @ColumnInfo("network_photo_count")
    val networkPhotoCount: Int,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorksiteNetworkFileCrossRef::class,
            parentColumn = "worksite_id",
            entityColumn = "network_file_id",
        ),
    )
    val fileImages: List<NetworkFileLocalImageEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val localImages: List<WorksiteLocalImageEntity>,
)

private val highPriorityFlagLiteral = WorksiteFlagType.HighPriority.literal
private val duplicateFlagLiteral = WorksiteFlagType.Duplicate.literal
fun PopulatedWorksiteMapVisual.asExternalModel(isFilteredOut: Boolean = false) = WorksiteMapMark(
    id = id,
    latitude = latitude,
    longitude = longitude,
    statusClaim = WorkTypeStatusClaim.make(keyWorkTypeStatus, keyWorkTypeOrgClaim),
    workType = WorkTypeStatusClaim.getType(keyWorkTypeType),
    workTypeCount = workTypeCount,
    // TODO Account for unsynced local favorites as well
    isFavorite = favoriteId != null,
    isHighPriority = flags.any {
        it.isHighPriority == true ||
            it.reasonT == highPriorityFlagLiteral
    },
    isDuplicate = flags.any { it.reasonT == duplicateFlagLiteral },
    isFilteredOut = isFilteredOut,
    hasPhotos = networkPhotoCount > 0 ||
        fileImages.any { !it.isDeleted } ||
        localImages.isNotEmpty(),
)

data class PopulatedNetworkIdWorksiteId(
    @ColumnInfo("network_id")
    val networkId: Long,
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
)
