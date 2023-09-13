package com.crisiscleanup.core.mapmarker

import com.crisiscleanup.core.model.data.CaseStatus
import com.crisiscleanup.core.model.data.CaseStatus.ClaimedNotStarted
import com.crisiscleanup.core.model.data.CaseStatus.Completed
import com.crisiscleanup.core.model.data.CaseStatus.DoneByOthersNhw
import com.crisiscleanup.core.model.data.CaseStatus.InProgress
import com.crisiscleanup.core.model.data.CaseStatus.Incomplete
import com.crisiscleanup.core.model.data.CaseStatus.NeedsFollowUp
import com.crisiscleanup.core.model.data.CaseStatus.OutOfScopeDu
import com.crisiscleanup.core.model.data.CaseStatus.PartiallyCompleted
import com.crisiscleanup.core.model.data.CaseStatus.Unclaimed
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedCompleted
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedDoneByOthers
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedDuplicate
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedIncomplete
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedNoHelpWanted
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedOutOfScope
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenAssigned
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenNeedsFollowUp
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenPartiallyCompleted
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenUnassigned
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenUnresponsive
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
    WorkTypeStatusClaim(ClosedDuplicate, true) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedDoneByOthers, true) to DoneByOthersNhw,
    WorkTypeStatusClaim(ClosedNoHelpWanted, true) to DoneByOthersNhw,
    WorkTypeStatusClaim(WorkTypeStatus.Unknown, false) to CaseStatus.Unknown,
    WorkTypeStatusClaim(OpenAssigned, false) to Unclaimed,
    WorkTypeStatusClaim(OpenUnassigned, false) to Unclaimed,
    WorkTypeStatusClaim(OpenPartiallyCompleted, false) to PartiallyCompleted,
    WorkTypeStatusClaim(OpenNeedsFollowUp, false) to NeedsFollowUp,
    WorkTypeStatusClaim(OpenUnresponsive, false) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedCompleted, false) to Completed,
    WorkTypeStatusClaim(ClosedIncomplete, false) to Incomplete,
    WorkTypeStatusClaim(ClosedOutOfScope, false) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedDuplicate, false) to OutOfScopeDu,
    WorkTypeStatusClaim(ClosedDoneByOthers, false) to DoneByOthersNhw,
    WorkTypeStatusClaim(ClosedNoHelpWanted, false) to DoneByOthersNhw,
)
