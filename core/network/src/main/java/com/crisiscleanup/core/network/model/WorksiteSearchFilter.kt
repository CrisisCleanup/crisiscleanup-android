package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.closedWorkTypeStatuses
import com.crisiscleanup.core.model.data.openWorkTypeStatuses
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CasesFilter.statusQueryString: String?
    get() {
        val statuses = workTypeStatuses.toMutableSet()
        if (isStatusOpen) {
            statuses.addAll(openWorkTypeStatuses)
        } else if (isStatusClosed) {
            statuses.addAll(closedWorkTypeStatuses)
        }

        return if (statuses.isEmpty()) null
        else workTypeStatuses
            .map(WorkTypeStatus::literal)
            .joinToString(",")
    }

private fun trueOrNull(b: Boolean): Boolean? {
    return if (b) true else null
}

private val CasesFilter.flagQueryString: String?
    get() = if (worksiteFlags.isEmpty()) null else worksiteFlags.joinToString(",")

private val CasesFilter.workQueryString: String?
    get() = if (workTypes.isEmpty()) null else workTypes.joinToString(",")


private val dateQueryStringFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd")
    .withZone(ZoneId.systemDefault())

private fun dateRangeQueryString(
    datePrefix: String,
    dateRange: Pair<Instant, Instant>?
): List<Pair<String, String>>? {
    if (dateRange == null) {
        return null
    }

    var minDate = dateRange.first
    var maxDate = dateRange.second
    if (minDate > maxDate) {
        val tempDate = minDate
        minDate = maxDate
        maxDate = tempDate
    }

    return listOf(
        Pair("${datePrefix}_at__gt", dateQueryStringFormatter.format(minDate.toJavaInstant())),
        Pair("&${datePrefix}_at__lt=", dateQueryStringFormatter.format(maxDate.toJavaInstant()))
    )
}

fun CasesFilter.queryMap(myOrgId: Long): Map<String, Any?> {
    // TODO svi, updated days ago, and distance

    val queryMap = mutableMapOf<String, Any?>(
        "organization_primary_location" to if (isWithinPrimaryResponseArea) myOrgId else null,
        "organization_secondary_location" to if (isWithinSecondaryResponseArea) myOrgId else null,
        "my_team" to trueOrNull(isAssignedToMyTeam),
        "work_type__claimed_by__isnull" to trueOrNull(isUnclaimed),
        "work_type__claimed_by" to trueOrNull(isClaimedByMyOrg),
        "reported_by" to trueOrNull(isReportedByMyOrg),
        "work_type__status__in" to statusQueryString,
        "member_of_my_organization" to trueOrNull(isMemberOfMyOrg),
        "older_than_60" to trueOrNull(isOlderThan60),
        "children_in_home" to trueOrNull(hasChildrenInHome),
        "first_responder" to trueOrNull(isFirstResponder),
        "veteran" to trueOrNull(isVeteran),
        "flags" to flagQueryString,
        "work_type__work_type__in" to workQueryString,
        "missing_work_type" to trueOrNull(isNoWorkType),
    )

    fun addDateQueryParams(prefix: String, dateRange: Pair<Instant, Instant>?) {
        dateRangeQueryString(prefix, dateRange)?.let { qs ->
            qs.forEach {
                queryMap[it.first] = it.second
            }
        }
    }
    addDateQueryParams("created", createdAt)
    addDateQueryParams("updated", updatedAt)

    return queryMap
}