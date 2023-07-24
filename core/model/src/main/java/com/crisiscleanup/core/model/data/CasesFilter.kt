package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

private const val defaultSvi = 1f
private const val defaultFilterDistance = 0f

const val CasesFilterMinDaysAgo: Int = 3
const val CasesFilterMaxDaysAgo: Int = 193
private const val CasesFilterDaysAgoDelta = CasesFilterMaxDaysAgo - CasesFilterMinDaysAgo
private const val defaultDaysAgo = CasesFilterMaxDaysAgo

data class CasesFilter(
    val svi: Float = defaultSvi,
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
    companion object {
        fun determineDaysAgo(daysAgoNormalized: Float) =
            CasesFilterMinDaysAgo + (daysAgoNormalized * CasesFilterDaysAgoDelta).toInt()
                .coerceIn(0, CasesFilterDaysAgoDelta)
    }

    private val isDefault = this == DefaultCasesFilter

    private val isUpdatedDaysAgoChanged: Boolean
        get() = daysAgoUpdated != defaultDaysAgo

    val isDistanceChanged: Boolean
        get() = distance != defaultFilterDistance

    val changeCount: Int
        get() {
            if (isDefault) return 0

            var count = 0
            if (svi != defaultSvi) count++
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
        get() = ((daysAgoUpdated.toFloat() - CasesFilterMinDaysAgo) / CasesFilterDaysAgoDelta).coerceIn(
            0f,
            1f
        )

    fun expandDaysAgo(daysAgoNormalized: Float) =
        copy(daysAgoUpdated = determineDaysAgo(daysAgoNormalized))

    /**
     * @return TRUE if values meet local filters or FALSE otherwise
     */
    fun localFilter(
        compareSvi: Float,
        updatedAt: Instant,
        haversineDistanceMiles: Double?,
    ): Boolean {
        if (compareSvi > svi) {
            return false
        }

        if (isUpdatedDaysAgoChanged) {
            val resultDaysAgoUpdate = Clock.System.now().minus(updatedAt).inWholeDays
            if (resultDaysAgoUpdate > daysAgoUpdated) {
                return false
            }
        }

        if (isDistanceChanged) {
            haversineDistanceMiles?.let {
                if (it > distance) {
                    return false
                }
            }
        }

        return true
    }
}

private val DefaultCasesFilter = CasesFilter()