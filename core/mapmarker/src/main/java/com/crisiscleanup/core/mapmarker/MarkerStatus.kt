package com.crisiscleanup.core.mapmarker

import com.crisiscleanup.core.model.data.CaseStatus
import com.crisiscleanup.core.model.data.CaseStatus.*
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorkTypeStatus.*
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim

internal val statusClaimToStatus = mapOf(
    WorkTypeStatusClaim(WorkTypeStatus.Unknown, true) to CaseStatus.Unknown,
    WorkTypeStatusClaim(OpenAssigned, true) to InProgress,
    WorkTypeStatusClaim(OpenUnassigned, true) to ClaimedNotStarted,
    WorkTypeStatusClaim(OpenPartiallyCompleted, true) to PartiallyCompleted,
    WorkTypeStatusClaim(OpenNeedsFollowUp, true) to NeedsFollowUp,
    WorkTypeStatusClaim(OpenUnresponsive, true) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedCompleted, true) to Completed,
    WorkTypeStatusClaim(ClosedIncomplete, true) to Incomplete,
    WorkTypeStatusClaim(ClosedOutOfScope, true) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedDoneByOthers, true) to DoneByOthersNhwPc,
    WorkTypeStatusClaim(WorkTypeStatus.Unknown, false) to CaseStatus.Unknown,
    WorkTypeStatusClaim(OpenAssigned, false) to Unclaimed,
    WorkTypeStatusClaim(OpenUnassigned, false) to Unclaimed,
    WorkTypeStatusClaim(OpenPartiallyCompleted, false) to PartiallyCompleted,
    WorkTypeStatusClaim(OpenNeedsFollowUp, false) to NeedsFollowUp,
    WorkTypeStatusClaim(OpenUnresponsive, false) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedCompleted, false) to Completed,
    WorkTypeStatusClaim(ClosedIncomplete, false) to Incomplete,
    WorkTypeStatusClaim(ClosedOutOfScope, false) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedDoneByOthers, false) to DoneByOthersNhwPc,
)
