package com.crisiscleanup.core.mapmarker

import androidx.compose.ui.graphics.Color
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.CaseStatus.*
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.WorkTypeStatusClaim

data class MapMarkerColor(
    private val fillLong: Long,
    private val strokeLong: Long = 0xFFFFFFFF,
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
    DoneByOthersNhwPc to MapMarkerColor(statusDoneByOthersNhwColorCode),
    // Unresponsive
    OutOfScopeDu to MapMarkerColor(statusOutOfScopeRejectedColorCode),
    Incomplete to MapMarkerColor(statusDoneByOthersNhwColorCode),
)

private val statusClaimMapMarkerColors = mapOf(
    WorkTypeStatusClaim(WorkTypeStatus.ClosedDuplicate, true) to MapMarkerColor(
        statusDuplicateClaimedColorCode
    ),
    WorkTypeStatusClaim(WorkTypeStatus.OpenPartiallyCompleted, false) to MapMarkerColor(
        statusUnclaimedColorCode
    ),
    WorkTypeStatusClaim(WorkTypeStatus.OpenNeedsFollowUp, false) to MapMarkerColor(
        statusUnclaimedColorCode
    ),
    WorkTypeStatusClaim(WorkTypeStatus.ClosedDuplicate, false) to MapMarkerColor(
        statusDuplicateUnclaimedColorCode
    ),
)

internal fun getMapMarkerColors(statusClaim: WorkTypeStatusClaim): MapMarkerColor {
    var colors = statusClaimMapMarkerColors[statusClaim]
    if (colors == null) {
        val status = statusClaimToStatus[statusClaim]
        colors = statusMapMarkerColors[status] ?: statusMapMarkerColors[Unknown]!!
    }
    return colors
}
