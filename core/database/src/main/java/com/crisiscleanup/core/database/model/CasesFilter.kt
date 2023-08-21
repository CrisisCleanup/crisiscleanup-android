package com.crisiscleanup.core.database.model

import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.common.kmToMiles
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.OrganizationLocationAreaBounds
import kotlinx.datetime.Instant

fun CasesFilter.passesFilter(
    organizationAffiliates: Set<Long>,
    flags: Collection<WorksiteFlagEntity>,
    formData: Collection<WorksiteFormDataEntity>,
    workTypes: Collection<WorkTypeEntity>,
    worksiteCreatedAt: Instant?,
    worksiteIsFavorite: Boolean,
    worksiteReportedBy: Long?,
    worksiteUpdatedAt: Instant,
    worksiteLatitude: Double,
    worksiteLongitude: Double,
    locationAreaBounds: OrganizationLocationAreaBounds,
): Boolean {
    if (hasWorkTypeFilters) {
        var assignedToMyTeamCount = 0
        var unclaimedCount = 0
        var claimedByMyOrgCount = 0
        var matchingStatusCount = 0
        var matchingWorkTypeCount = 0

        workTypes.forEach { workType ->
            if (workType.orgClaim == null) {
                unclaimedCount++
            } else {
                val orgClaim = workType.orgClaim
                if (organizationAffiliates.contains(orgClaim)) {
                    assignedToMyTeamCount++
                }
                if (organizationAffiliates.contains(orgClaim)) {
                    claimedByMyOrgCount++
                }
            }

            if (matchingStatuses.contains(workType.status)) {
                matchingStatusCount++
            }

            if (matchingWorkTypes.contains(workType.workType)) {
                matchingWorkTypeCount++
            }
        }

        if (isAssignedToMyTeam && assignedToMyTeamCount == 0) {
            return false
        }

        if (isUnclaimed && unclaimedCount == 0) {
            return false
        }

        if (isClaimedByMyOrg && claimedByMyOrgCount == 0) {
            return false
        }

        if (matchingStatuses.isNotEmpty() && matchingStatusCount == 0) {
            return false
        }

        if (matchingWorkTypes.isNotEmpty() && matchingWorkTypeCount == 0) {
            return false
        }
    }

    if (isReportedByMyOrg) {
        worksiteReportedBy?.let {
            if (!organizationAffiliates.contains(it)) {
                return false
            }
        }
    }

    if (isMemberOfMyOrg && !worksiteIsFavorite) {
        return false
    }

    val formDataFilters = matchingFormData
    if (formDataFilters.isNotEmpty()) {
        if (!formData.any {
                formDataFilters.contains(it.fieldKey) && it.isBoolValue
            }
        ) {
            return false
        }
    }

    if (worksiteFlags.isNotEmpty()) {
        if (!flags.any { matchingFlags.contains(it.reasonT) }) {
            return false
        }
    }

    createdAt?.let { (min, max) ->
        worksiteCreatedAt?.let { at ->
            if (at < min || at > max) {
                return false
            }
        }
    }

    updatedAt?.let { (min, max) ->
        if (worksiteUpdatedAt < min || worksiteUpdatedAt > max) {
            return false
        }
    }

    if (isWithinPrimaryResponseArea) {
        locationAreaBounds.primary?.let {
            if (!it.isInBounds(worksiteLatitude, worksiteLongitude)) {
                return false
            }
        }
    }

    if (isWithinSecondaryResponseArea) {
        locationAreaBounds.secondary?.let {
            if (!it.isInBounds(worksiteLatitude, worksiteLongitude)) {
                return false
            }
        }
    }

    return true
}

internal fun CasesFilter.passes(
    worksite: WorksiteEntity,
    flagEntities: List<WorksiteFlagEntity>,
    formDataEntities: List<WorksiteFormDataEntity>,
    workTypeEntities: List<WorkTypeEntity>,
    organizationAffiliates: Set<Long>,
    latRad: Double?,
    lngRad: Double?,
    isFavorite: Boolean,
    locationAreaBounds: OrganizationLocationAreaBounds,
): Boolean {
    if (isDefault) {
        return true
    }

    val filterByDistance = latRad != null && lngRad != null && hasDistanceFilter
    val distance = if (filterByDistance) {
        haversineDistance(
            latRad!!,
            lngRad!!,
            worksite.latitude.radians,
            worksite.longitude.radians,
        ).kmToMiles
    } else {
        0.0
    }
    if (!passesFilter(
            worksite.svi ?: 0f,
            worksite.updatedAt,
            distance,
        )
    ) {
        return false
    }

    if (hasAdditionalFilters &&
        !passesFilter(
            organizationAffiliates,
            flagEntities,
            formDataEntities,
            workTypeEntities,
            worksite.createdAt,
            isFavorite,
            worksite.reportedBy,
            worksite.updatedAt,
            worksite.latitude,
            worksite.longitude,
            locationAreaBounds,
        )
    ) {
        return false
    }

    return true
}
