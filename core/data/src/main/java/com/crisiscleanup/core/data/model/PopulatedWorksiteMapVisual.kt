package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.common.kmToMiles
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.passesFilter
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.WorksiteMapMark

fun List<PopulatedWorksiteMapVisual>.filter(
    filters: CasesFilter,
    organizationAffiliates: Set<Long>,
    location: Pair<Double, Double>? = null,
): List<WorksiteMapMark> {
    if (filters.isDefault) {
        return map(PopulatedWorksiteMapVisual::asExternalModel)
    }

    val filterByDistance = location != null && filters.hasDistanceFilter
    val latRad = if (filterByDistance) location!!.first.radians else 0.0
    val lngRad = if (filterByDistance) location!!.second.radians else 0.0
    return mapNotNull {
        val distance = if (filterByDistance) {
            haversineDistance(
                latRad, lngRad,
                it.latitude.radians, it.longitude.radians,
            ).kmToMiles
        } else {
            0.0
        }
        if (!filters.passesFilter(
                it.svi ?: 0f,
                it.updatedAt,
                distance,
            )
        ) {
            return@mapNotNull null
        }

        val isFilteredOut = filters.hasAdditionalFilters &&
                !filters.passesFilter(
                    organizationAffiliates,
                    it.flags,
                    it.formData,
                    it.workTypes,
                    it.createdAt,
                    it.isFavorite,
                    it.reportedBy,
                    it.updatedAt,
                )
        it.asExternalModel(isFilteredOut)
    }
}

private val PopulatedWorksiteMapVisual.isFavorite: Boolean
    get() {
        return if (root.isLocalModified) isLocalFavorite else favoriteId != null
    }
