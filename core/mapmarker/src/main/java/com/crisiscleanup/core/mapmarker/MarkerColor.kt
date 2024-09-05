package com.crisiscleanup.core.mapmarker

import androidx.compose.ui.graphics.Color
import com.crisiscleanup.core.designsystem.theme.STATUS_COMPLETED_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_DONE_BY_OTHERS_NHW_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_DUPLICATE_CLAIMED_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_DUPLICATE_UNCLAIMED_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_IN_PROGRESS_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_NEEDS_FOLLOW_UP_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_NOT_STARTED_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_OUT_OF_SCOPE_REJECTED_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_PARTIALLY_COMPLETED_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_UNCLAIMED_COLOR_CODE
import com.crisiscleanup.core.designsystem.theme.STATUS_UNKNOWN_COLOR_CODE
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
    Unknown to MapMarkerColor(STATUS_UNKNOWN_COLOR_CODE),
    Unclaimed to MapMarkerColor(STATUS_UNCLAIMED_COLOR_CODE),
    ClaimedNotStarted to MapMarkerColor(STATUS_NOT_STARTED_COLOR_CODE),
    // Assigned
    InProgress to MapMarkerColor(STATUS_IN_PROGRESS_COLOR_CODE),
    PartiallyCompleted to MapMarkerColor(STATUS_PARTIALLY_COMPLETED_COLOR_CODE),
    NeedsFollowUp to MapMarkerColor(STATUS_NEEDS_FOLLOW_UP_COLOR_CODE),
    Completed to MapMarkerColor(STATUS_COMPLETED_COLOR_CODE),
    DoneByOthersNhw to MapMarkerColor(STATUS_DONE_BY_OTHERS_NHW_COLOR_CODE),
    // Unresponsive
    OutOfScopeDu to MapMarkerColor(STATUS_OUT_OF_SCOPE_REJECTED_COLOR_CODE),
    Incomplete to MapMarkerColor(STATUS_DONE_BY_OTHERS_NHW_COLOR_CODE),
)

private val statusClaimMapMarkerColors = mapOf(
    WorkTypeStatusClaim(WorkTypeStatus.ClosedDuplicate, true) to MapMarkerColor(
        STATUS_DUPLICATE_CLAIMED_COLOR_CODE,
    ),
    WorkTypeStatusClaim(WorkTypeStatus.OpenPartiallyCompleted, false) to MapMarkerColor(
        STATUS_UNCLAIMED_COLOR_CODE,
    ),
    WorkTypeStatusClaim(WorkTypeStatus.OpenNeedsFollowUp, false) to MapMarkerColor(
        STATUS_UNCLAIMED_COLOR_CODE,
    ),
    WorkTypeStatusClaim(WorkTypeStatus.ClosedDuplicate, false) to MapMarkerColor(
        STATUS_DUPLICATE_UNCLAIMED_COLOR_CODE,
    ),
)

internal const val FILTERED_OUT_MARKER_ALPHA = 0.2f
private const val FILTERED_OUT_MARKER_STROKE_ALPHA = 1.0f
private const val FILTERED_OUT_MARKER_FILL_ALPHA = 0.25f
private const val FILTERED_OUT_DOT_STROKE_ALPHA = 0.5f
private const val FILTERED_OUT_DOT_FILL_ALPHA = 0.1f
private const val DUPLICATE_MARKER_ALPHA = 0.3f

internal fun getMapMarkerColors(
    statusClaim: WorkTypeStatusClaim,
): MapMarkerColor {
    var colors = statusClaimMapMarkerColors[statusClaim]
    if (colors == null) {
        val status = statusClaimToStatus[statusClaim]
        colors = statusMapMarkerColors[status] ?: statusMapMarkerColors[Unknown]!!
    }

    return colors
}

internal fun getMapMarkerColors(
    statusClaim: WorkTypeStatusClaim,
    isDuplicate: Boolean,
    isFilteredOut: Boolean,
    isVisited: Boolean,
    isDot: Boolean,
): MapMarkerColor {
    var colors = getMapMarkerColors(statusClaim)

    if (isDuplicate) {
        colors = colors.copy(
            fill = colors.fill.copy(alpha = DUPLICATE_MARKER_ALPHA),
            stroke = colors.stroke.copy(alpha = DUPLICATE_MARKER_ALPHA),
        )
    } else if (isFilteredOut) {
        val fillAlpha = if (isDot) FILTERED_OUT_DOT_FILL_ALPHA else FILTERED_OUT_MARKER_FILL_ALPHA
        val strokeAlpha =
            if (isDot) FILTERED_OUT_DOT_STROKE_ALPHA else FILTERED_OUT_MARKER_STROKE_ALPHA
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
