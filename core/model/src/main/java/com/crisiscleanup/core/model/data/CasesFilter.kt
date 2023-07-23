package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

private const val defaultFilterDistance = 100f

const val CasesFilterMinDaysAgo: Int = 3
const val CasesFilterMaxDaysAgo: Int = 193
private const val defaultDaysAgo = CasesFilterMaxDaysAgo

data class CasesFilter(
    val svi: Float = 0f,
    val daysAgoUpdated: Int = defaultDaysAgo,
    /**
     * In miles. 0 is any distance.
     */
    val distance: Float = defaultFilterDistance,
    val isWithinPrimaryResponseArea: Boolean = false,
    val isWithinSecondaryResponseArea: Boolean = false,
    val isAssignedToMyTeam: Boolean = false,
    val isUnclaimed: Boolean = false,
    val isClaimedByMyOrg: Boolean = false,
    val isReportedByMyOrg: Boolean = false,
    val isStatusOpen: Boolean = false,
    val isStatusClosed: Boolean = false,
    val workTypeStatuses: Collection<WorkTypeStatus> = emptySet(),
    val isMemberOfMyOrg: Boolean = false,
    val isOlderThan60: Boolean = false,
    val hasChildrenInHome: Boolean = false,
    val isFirstResponder: Boolean = false,
    val isVeteran: Boolean = false,
    val worksiteFlags: Collection<WorksiteFlagType> = emptySet(),
    val workTypes: Collection<String> = emptySet(),
    val isNoWorkType: Boolean = false,
    val createdAt: Pair<Instant, Instant>? = null,
    val updatedAt: Pair<Instant, Instant>? = null,
) {
    private val isDefault = this == DefaultCasesFilter

    val changeCount: Int
        get() {
            if (isDefault) return 0

            var count = 0
            if (svi > 0) count++
            if (daysAgoUpdated != defaultDaysAgo) count++
            if (distance != defaultFilterDistance) count++
            if (isWithinPrimaryResponseArea) count++
            if (isWithinSecondaryResponseArea) count++
            if (isAssignedToMyTeam) count++
            if (isUnclaimed) count++
            if (isClaimedByMyOrg) count++
            if (isReportedByMyOrg) count++
            if (isStatusOpen) count++
            if (isStatusClosed) count++
            if (workTypeStatuses.isNotEmpty()) count += workTypeStatuses.size
            if (isMemberOfMyOrg) count++
            if (isOlderThan60) count++
            if (hasChildrenInHome) count++
            if (isFirstResponder) count++
            if (isVeteran) count++
            if (worksiteFlags.isNotEmpty()) count += worksiteFlags.size
            if (workTypes.isNotEmpty()) count += workTypes.size
            if (isNoWorkType) count++
            if (createdAt != null) count++
            if (updatedAt != null) count++

            return count
        }

    val daysAgoNormalized
        get() = (daysAgoUpdated.toFloat() / CasesFilterMaxDaysAgo).coerceIn(
            0f,
            1f
        )

    fun expandDaysAgo(daysAgoNormalized: Float): CasesFilter {
        val daysAgo = (daysAgoNormalized * CasesFilterMaxDaysAgo).toInt()
            .coerceIn(CasesFilterMinDaysAgo, CasesFilterMaxDaysAgo)
        return copy(daysAgoUpdated = daysAgo)
    }
}

private val DefaultCasesFilter = CasesFilter()