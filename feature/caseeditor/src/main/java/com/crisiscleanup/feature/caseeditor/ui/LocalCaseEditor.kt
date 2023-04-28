package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorkTypeStatus.*

data class CaseEditor(
    val statusOptions: List<WorkTypeStatus>,
)

val statusOptionColors = mapOf(
    Unknown to statusUnknownColor,
    OpenAssigned to statusInProgressColor,
    OpenUnassigned to statusNotStartedColor,
    OpenPartiallyCompleted to statusPartiallyCompletedColor,
    OpenNeedsFollowUp to statusNeedsFollowUpColor,
    OpenUnresponsive to statusUnresponsiveColor,
    ClosedCompleted to statusCompletedColor,
    ClosedIncomplete to statusDoneByOthersNhwDiColor,
    ClosedOutOfScope to statusOutOfScopeRejectedColor,
    ClosedDoneByOthers to statusDoneByOthersNhwDiColor,
    ClosedNoHelpWanted to statusDoneByOthersNhwDiColor,
    ClosedDuplicate to statusDoneByOthersNhwDiColor,
    ClosedRejected to statusOutOfScopeRejectedColor,
)

private val caseEditor = CaseEditor(
    statusOptions = emptyList(),
)

val LocalCaseEditor = staticCompositionLocalOf { caseEditor }