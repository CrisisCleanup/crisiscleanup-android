package com.crisiscleanup.core.mapmarker

import androidx.compose.ui.graphics.Color
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.CaseStatus.*

data class MapMarkerColor(
    private val fillLong: Long,
    private val strokeLong: Long = 0xFFFFFFFF,
    val fillInt: Int = fillLong.toInt(),
    val strokeInt: Int = strokeLong.toInt(),
    val fill: Color = Color(fillLong),
    val stroke: Color = Color(strokeLong),
)

internal val mapMarkerColors = mapOf(
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
    OutOfScope to MapMarkerColor(statusOutOfScopeRejectedColorCode),
    Incomplete to MapMarkerColor(statusDoneByOthersNhwColorCode),
)
