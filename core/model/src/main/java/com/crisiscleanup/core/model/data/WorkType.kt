package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class WorkType(
    val id: Long,
    val createdAt: Instant? = null,
    val orgClaim: Long? = null,
    val nextRecurAt: Instant? = null,
    val phase: Int? = null,
    val recur: String? = null,
    val statusLiteral: String,
    val workType: String,
    val isClaimed: Boolean = orgClaim != null,
) {
    val status: WorkTypeStatus = statusFromLiteral(statusLiteral)
    val statusClaim = WorkTypeStatusClaim(status, isClaimed)
}

private fun statusFromLiteral(status: String) = when (status.lowercase()) {
    "open_unassigned" -> WorkTypeStatus.OpenUnassigned
    "open_assigned" -> WorkTypeStatus.OpenAssigned
    "open_partially-completed" -> WorkTypeStatus.OpenPartiallyCompleted
    "open_needs-follow-up" -> WorkTypeStatus.OpenNeedsFollowUp
    "open_unresponsive" -> WorkTypeStatus.OpenUnresponsive
    "closed_completed" -> WorkTypeStatus.ClosedCompleted
    "closed_incomplete" -> WorkTypeStatus.ClosedIncomplete
    "closed_out-of-scope" -> WorkTypeStatus.ClosedOutOfScope
    "closed_done-by-others" -> WorkTypeStatus.ClosedDoneByOthers
    else -> WorkTypeStatus.Unknown
}

enum class WorkTypeStatus {
    Unknown,
    OpenAssigned,
    OpenUnassigned,
    OpenPartiallyCompleted,
    OpenNeedsFollowUp,
    OpenUnresponsive,
    ClosedCompleted,
    ClosedIncomplete,
    ClosedOutOfScope,
    ClosedDoneByOthers,
}

data class WorkTypeStatusClaim(
    val status: WorkTypeStatus,
    val isClaimed: Boolean,
) {
    companion object {
        fun make(status: String, orgId: Long?) = WorkTypeStatusClaim(
            statusFromLiteral(status),
            isClaimed = orgId != null,
        )
    }
}