package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

private const val DEFAULT_SVI = 1f
private const val DEFAULT_FILTER_DISTANCE = 0f

const val CasesFilterMinDaysAgo: Int = 3
const val CasesFilterMaxDaysAgo: Int = 193
private const val CASES_FILTER_ADD_DAYS_DELTA = CasesFilterMaxDaysAgo - CasesFilterMinDaysAgo
private const val DEFAULT_DAYS_AGO = CasesFilterMaxDaysAgo

data class CasesFilter(
    val svi: Float = DEFAULT_SVI,
    val daysAgoUpdated: Int = DEFAULT_DAYS_AGO,
    /**
     * In miles. 0 is any distance.
     */
    val distance: Float = DEFAULT_FILTER_DISTANCE,
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
    companion object {
        fun determineDaysAgo(daysAgoNormalized: Float) =
            CasesFilterMinDaysAgo + (daysAgoNormalized * CASES_FILTER_ADD_DAYS_DELTA).toInt()
                .coerceIn(0, CASES_FILTER_ADD_DAYS_DELTA)
    }

    val isDefault = this == DefaultCasesFilter

    val hasSviFilter = svi != DEFAULT_SVI
    val hasUpdatedFilter = daysAgoUpdated != DEFAULT_DAYS_AGO
    val hasDistanceFilter = distance != DEFAULT_FILTER_DISTANCE

    val changeCount by lazy {
        if (isDefault) return@lazy 0

        var count = 0
        if (hasSviFilter) count++
        if (daysAgoUpdated != DEFAULT_DAYS_AGO) count++
        if (distance != DEFAULT_FILTER_DISTANCE) count++
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

        count
    }

    val daysAgoNormalized by lazy {
        ((daysAgoUpdated.toFloat() - CasesFilterMinDaysAgo) / CASES_FILTER_ADD_DAYS_DELTA).coerceIn(
            0f,
            1f,
        )
    }

    fun expandDaysAgo(daysAgoNormalized: Float) =
        copy(daysAgoUpdated = determineDaysAgo(daysAgoNormalized))

    /**
     * @return TRUE if values meet local filters or FALSE otherwise
     */
    fun passesFilter(
        compareSvi: Float,
        updatedAt: Instant,
        haversineDistanceMiles: Double?,
    ): Boolean {
        if (hasSviFilter && compareSvi > svi) {
            return false
        }

        if (hasUpdatedFilter && updatedAt.plus(daysAgoUpdated.days) < Clock.System.now()) {
            return false
        }

        if (hasDistanceFilter) {
            haversineDistanceMiles?.let {
                if (it > distance) {
                    return false
                }
            }
        }

        return true
    }

    val hasAdditionalFilters by lazy {
        var initialFilterCount = 0
        if (hasSviFilter) initialFilterCount++
        if (hasUpdatedFilter) initialFilterCount++
        if (hasDistanceFilter) initialFilterCount++
        changeCount > initialFilterCount
    }

    // TODO How to determine is within primary/secondary areas?

    val hasWorkTypeFilters by lazy {
        isAssignedToMyTeam ||
                isUnclaimed ||
                isClaimedByMyOrg ||
                isStatusOpen ||
                isStatusClosed ||
                workTypeStatuses.isNotEmpty() ||
                workTypes.isNotEmpty()
    }

    val matchingStatuses: Set<String> by lazy {
        val statuses = mutableSetOf<WorkTypeStatus>()
        statuses.addAll(workTypeStatuses)
        if (isStatusOpen) {
            statuses.addAll(openWorkTypeStatuses)
        }
        if (isStatusClosed) {
            statuses.addAll(closedWorkTypeStatuses)
        }
        statuses.map(WorkTypeStatus::literal).toSet()
    }

    val matchingWorkTypes: Set<String> by lazy {
        workTypes.toSet()
    }

    val matchingFormData: Set<String> by lazy {
        val formData = mutableSetOf<String>()
        if (isOlderThan60) {
            formData.add("older_than_60")
        }
        if (hasChildrenInHome) {
            formData.add("children_in_home")
        }
        if (isFirstResponder) {
            formData.add("first_responder")
        }
        if (isVeteran) {
            formData.add("veteran")
        }

        formData.toSet()
    }

    val matchingFlags: Set<String> by lazy {
        worksiteFlags.map(WorksiteFlagType::literal).toSet()
    }

    // TODO How to determine no work type?
}

private val DefaultCasesFilter = CasesFilter()