package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.crisiscleanup.core.common.radians
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
    val filterByDistance = location != null && filters.hasDistanceFilter
    val latRad = if (filterByDistance) location!!.first.radians else null
    val lngRad = if (filterByDistance) location!!.second.radians else null
    return mapNotNull {
        if (filters.passes(
                it.base.entity,
                it.flags,
                it.formData,
                it.base.workTypes,
                organizationAffiliates,
                latRad,
                lngRad,
                it.base.isFavorite,
            )
        ) it else null
    }
}
