package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.CasesFilterMaxDaysAgo
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.core.model.data.flagFromLiteral
import com.crisiscleanup.core.model.data.statusFromLiteral
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class CasesFiltersDataSource @Inject constructor(
    private val dataStore: DataStore<LocalPersistedCasesFilters>,
) {
    private fun getDateRage(startSeconds: Long, endSeconds: Long) =
        if (startSeconds in 1..endSeconds) {
            Pair(
                Instant.fromEpochSeconds(startSeconds),
                Instant.fromEpochSeconds(endSeconds),
            )
        } else {
            null
        }

    val casesFilters = dataStore.data.map {
        with(it) {
            val statuses = workTypeStatusesMap.keys
                .map { key -> statusFromLiteral(key) }
                .filter { status -> status != WorkTypeStatus.Unknown }
                .toSet()
            val flags = worksiteFlagsMap.keys
                .mapNotNull { key -> flagFromLiteral(key) }
                .toSet()
            val wts = workTypesMap.keys
                .toSet()
            val createdAt = getDateRage(createdAtStartSeconds, createdAtEndSeconds)
            val updatedAt = getDateRage(updatedAtStartSeconds, updatedAtEndSeconds)
            val isUnfiltered = daysAgoUpdated <= 0
            CasesFilter(
                svi = if (isUnfiltered) 1.0f else svi,
                daysAgoUpdated = if (isUnfiltered) CasesFilterMaxDaysAgo else daysAgoUpdated,
                distance = distance,
                isWithinPrimaryResponseArea = isWithinPrimaryResponseArea,
                isWithinSecondaryResponseArea = isWithinSecondaryResponseArea,
                isAssignedToMyTeam = isAssignedToMyTeam,
                isUnclaimed = isUnclaimed,
                isClaimedByMyOrg = isClaimedByMyOrg,
                isReportedByMyOrg = isReportedByMyOrg,
                isStatusOpen = isStatusOpen,
                isStatusClosed = isStatusClosed,
                workTypeStatuses = statuses,
                isMemberOfMyOrg = isMemberOfMyOrg,
                isOlderThan60 = isOlderThan60,
                hasChildrenInHome = hasChildrenInHome,
                isFirstResponder = isFirstResponder,
                isVeteran = isVeteran,
                worksiteFlags = flags,
                workTypes = wts,
                isNoWorkType = isNoWorkType,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }

    suspend fun updateFilters(filters: CasesFilter) {
        val statuses = filters.workTypeStatuses.map(WorkTypeStatus::literal)
            .associateWith { true }
        val flags = filters.worksiteFlags.map(WorksiteFlagType::literal)
            .associateWith { true }
        val wts = filters.workTypes.associateWith { true }
        dataStore.updateData {
            it.copy {
                svi = filters.svi
                daysAgoUpdated = filters.daysAgoUpdated
                distance = filters.distance
                isWithinPrimaryResponseArea = filters.isWithinPrimaryResponseArea
                isWithinSecondaryResponseArea = filters.isWithinSecondaryResponseArea
                isAssignedToMyTeam = filters.isAssignedToMyTeam
                isUnclaimed = filters.isUnclaimed
                isClaimedByMyOrg = filters.isClaimedByMyOrg
                isReportedByMyOrg = filters.isReportedByMyOrg
                isStatusOpen = filters.isStatusOpen
                isStatusClosed = filters.isStatusClosed
                workTypeStatuses.clear()
                workTypeStatuses.putAll(statuses)
                isMemberOfMyOrg = filters.isMemberOfMyOrg
                isOlderThan60 = filters.isOlderThan60
                hasChildrenInHome = filters.hasChildrenInHome
                isFirstResponder = filters.isFirstResponder
                isVeteran = filters.isVeteran
                worksiteFlags.clear()
                worksiteFlags.putAll(flags)
                workTypes.clear()
                workTypes.putAll(wts)
                isNoWorkType = filters.isNoWorkType
                createdAtStartSeconds = filters.createdAt?.first?.epochSeconds ?: 0
                createdAtEndSeconds = filters.createdAt?.second?.epochSeconds ?: 0
                updatedAtStartSeconds = filters.updatedAt?.first?.epochSeconds ?: 0
                updatedAtEndSeconds = filters.updatedAt?.second?.epochSeconds ?: 0
            }
        }
    }
}
