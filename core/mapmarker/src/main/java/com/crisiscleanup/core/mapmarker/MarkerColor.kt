package com.crisiscleanup.core.mapmarker

import androidx.compose.ui.graphics.Color
import com.crisiscleanup.core.designsystem.theme.statusCompletedColorCode
import com.crisiscleanup.core.designsystem.theme.statusDoneByOthersNhwColorCode
import com.crisiscleanup.core.designsystem.theme.statusDuplicateClaimedColorCode
import com.crisiscleanup.core.designsystem.theme.statusDuplicateUnclaimedColorCode
import com.crisiscleanup.core.designsystem.theme.statusInProgressColorCode
import com.crisiscleanup.core.designsystem.theme.statusNeedsFollowUpColorCode
import com.crisiscleanup.core.designsystem.theme.statusNotStartedColorCode
import com.crisiscleanup.core.designsystem.theme.statusOutOfScopeRejectedColorCode
import com.crisiscleanup.core.designsystem.theme.statusPartiallyCompletedColorCode
import com.crisiscleanup.core.designsystem.theme.statusUnclaimedColorCode
import com.crisiscleanup.core.designsystem.theme.statusUnknownColorCode
import com.crisiscleanup.core.designsystem.theme.visitedCaseMarkerColorCode
import com.crisiscleanup.core.model.data.CaseStatus.ClaimedNotStarted
import com.crisiscleanup.core.model.data.CaseStatus.Completed
import com.crisiscleanup.core.model.data.CaseStatus.DoneByOthersNhw
import com.crisiscleanup.core.model.data.CaseStatus.InProgress
import com.crisiscleanup.core.model.data.CaseStatus.Incomplete
import com.crisiscleanup.core.model.data.CaseStatus.NeedsFollowUp
import com.crisiscleanup.core.model.data.CaseStatus.OutOfScopeDu
import com.crisiscleanup.core.model.data.CaseStatus.PartiallyCompleted
import com.crisiscleanup.core.model.data.CaseStatus.Unclaimed
import com.crisiscleanup.core.model.data.CaseStatus.Unknown
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim

data class MapMarkerColor(
    val fillLong: Long,
    val strokeLong: Long = 0xFFFFFFFF,
    val fillInt: Int = fillLong.toInt(),
    val strokeInt: Int = strokeLong.toInt(),
    val fill: Color = Color(fillLong),
    val stroke: Color = Color(strokeLong),
)

private val statusMapMarkerColors = mapOf(
    Unknown to MapMarkerColor(statusUnknownColorCode),
    Unclaimed to MapMarkerColor(statusUnclaimedColorCode),
    ClaimedNotStarted to MapMarkerColor(statusNotStartedColorCode),
    // Assigned
    InProgress to MapMarkerColor(statusInProgressColorCode),
    PartiallyCompleted to MapMarkerColor(statusPartiallyCompletedColorCode),
    NeedsFollowUp to MapMarkerColor(statusNeedsFollowUpColorCode),
    Completed to MapMarkerColor(statusCompletedColorCode),
    DoneByOthersNhw to MapMarkerColor(statusDoneByOthersNhwColorCode),
    // Unresponsive
    OutOfScopeDu to MapMarkerColor(statusOutOfScopeRejectedColorCode),
    Incomplete to MapMarkerColor(statusDoneByOthersNhwColorCode),
)

private val statusClaimMapMarkerColors = mapOf(
    WorkTypeStatusClaim(WorkTypeStatus.ClosedDuplicate, true) to MapMarkerColor(
        statusDuplicateClaimedColorCode,
    ),
    WorkTypeStatusClaim(WorkTypeStatus.OpenPartiallyCompleted, false) to MapMarkerColor(
        statusUnclaimedColorCode,
    ),
    WorkTypeStatusClaim(WorkTypeStatus.OpenNeedsFollowUp, false) to MapMarkerColor(
        statusUnclaimedColorCode,
    ),
    WorkTypeStatusClaim(WorkTypeStatus.ClosedDuplicate, false) to MapMarkerColor(
        statusDuplicateUnclaimedColorCode,
    ),
)

internal const val filteredOutMarkerAlpha = 0.2f
private const val filteredOutMarkerStrokeAlpha = 1.0f
private const val filteredOutMarkerFillAlpha = 0.25f
private const val filteredOutDotStrokeAlpha = 0.5f
private const val filteredOutDotFillAlpha = 0.1f
private const val duplicateMarkerAlpha = 0.3f

internal fun getMapMarkerColors(
    statusClaim: WorkTypeStatusClaim,
    isDuplicate: Boolean,
    isFilteredOut: Boolean,
    isVisited: Boolean,
    isDot: Boolean,
): MapMarkerColor {
    var colors = statusClaimMapMarkerColors[statusClaim]
    if (colors == null) {
        val status = statusClaimToStatus[statusClaim]
        colors = statusMapMarkerColors[status] ?: statusMapMarkerColors[Unknown]!!
    }

    if (isDuplicate) {
        colors = colors.copy(
            fill = colors.fill.copy(alpha = duplicateMarkerAlpha),
            stroke = colors.stroke.copy(alpha = duplicateMarkerAlpha),
        )
    } else if (isFilteredOut) {
        val fillAlpha = if (isDot) filteredOutDotFillAlpha else filteredOutMarkerFillAlpha
        val strokeAlpha = if (isDot) filteredOutDotStrokeAlpha else filteredOutMarkerStrokeAlpha
        colors = MapMarkerColor(0xFFFFFFFF, colors.fillLong)
            .let {
                it.copy(
                    fill = it.fill.copy(alpha = fillAlpha),
                    stroke = it.stroke.copy(alpha = strokeAlpha),
                )
            }
    } else if (isVisited) {
        colors = MapMarkerColor(
            colors.fillLong,
            strokeLong = visitedCaseMarkerColorCode,
        )
    }

    return colors
}
