package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.model.data.CasesFilter
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
            }) {
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

    return true
}