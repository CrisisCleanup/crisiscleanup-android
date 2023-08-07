package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.model.data.CasesFilter

data class PopulatedTableDataWorksite(
    @Embedded
    val base: PopulatedWorksite,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val workTypeRequests: List<WorkTypeTransferRequestEntity>,

    // For filtering
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val formData: List<WorksiteFormDataEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "worksite_id",
    )
    val flags: List<WorksiteFlagEntity>,
)

fun PopulatedTableDataWorksite.asExternalModel() = base.asExternalModel()
    .copy(workTypeRequests = workTypeRequests.map(WorkTypeTransferRequestEntity::asExternalModel))

fun List<PopulatedTableDataWorksite>.filter(
    filters: CasesFilter,
    organizationAffiliates: Set<Long>,
    location: Pair<Double, Double>? = null,
): List<PopulatedTableDataWorksite> {
    if (filters.isDefault) {
        return this
    }

    return mapNotNull {
        val worksite = it.base.entity

        var distance = 0.0
        if (filters.hasDistanceFilter) {
            location?.let { (lat, lng) ->
                distance = haversineDistance(
                    lat, lng,
                    worksite.latitude, worksite.longitude,
                )
            }
        }
        if (!filters.passesFilter(
                worksite.svi ?: 0f,
                worksite.updatedAt,
                distance,
            )
        ) {
            return@mapNotNull null
        }

        if (filters.hasAdditionalFilters &&
            !filters.passesFilter(
                organizationAffiliates,
                it.flags,
                it.formData,
                it.base.workTypes,
                worksite.createdAt,
                it.base.isFavorite,
                worksite.reportedBy,
                worksite.updatedAt,
            )
        ) {
            return@mapNotNull null
        }

        it
    }
}

private val PopulatedWorksite.isFavorite: Boolean
    get() {
        return if (root.isLocalModified) entity.isLocalFavorite else entity.favoriteId != null
    }
