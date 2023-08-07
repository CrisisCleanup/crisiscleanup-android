package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.WorksiteMapMark

fun List<PopulatedWorksiteMapVisual>.filter(
    repository: CasesFilterRepository,
    organizationAffiliates: Set<Long>,
    location: Pair<Double, Double>? = null,
): List<WorksiteMapMark> {
    val filters = repository.casesFilters.value
    if (filters.isDefault) {
        return map(PopulatedWorksiteMapVisual::asExternalModel)
    }

    return mapNotNull {
        var distance = 0.0
        if (filters.hasDistanceFilter) {
            location?.let { (lat, lng) ->
                distance = haversineDistance(
                    lat, lng,
                    it.latitude, it.longitude,
                )
            }
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
        return if (root.isLocalModified) {
            isLocalFavorite
        } else {
            favoriteId != null
        }
    }
