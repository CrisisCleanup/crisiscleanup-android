package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.crisiscleanup.core.designsystem.theme.statusCompletedColor
import com.crisiscleanup.core.designsystem.theme.statusDoneByOthersNhwDiColor
import com.crisiscleanup.core.designsystem.theme.statusInProgressColor
import com.crisiscleanup.core.designsystem.theme.statusNeedsFollowUpColor
import com.crisiscleanup.core.designsystem.theme.statusNotStartedColor
import com.crisiscleanup.core.designsystem.theme.statusOutOfScopeRejectedColor
import com.crisiscleanup.core.designsystem.theme.statusPartiallyCompletedColor
import com.crisiscleanup.core.designsystem.theme.statusUnknownColor
import com.crisiscleanup.core.designsystem.theme.statusUnresponsiveColor
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedCompleted
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedDoneByOthers
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedDuplicate
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedIncomplete
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedNoHelpWanted
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedOutOfScope
import com.crisiscleanup.core.model.data.WorkTypeStatus.ClosedRejected
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenAssigned
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenNeedsFollowUp
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenPartiallyCompleted
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenUnassigned
import com.crisiscleanup.core.model.data.WorkTypeStatus.OpenUnresponsive
import com.crisiscleanup.core.model.data.WorkTypeStatus.Unknown

data class CaseEditor(
    val isEditable: Boolean,
    val statusOptions: List<WorkTypeStatus>,
    val isNewCase: Boolean,
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
    isEditable = false,
    statusOptions = emptyList(),
    isNewCase = false,
)

val LocalCaseEditor = staticCompositionLocalOf { caseEditor }
