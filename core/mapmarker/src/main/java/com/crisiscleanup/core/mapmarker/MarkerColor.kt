package com.crisiscleanup.core.mapmarker

import androidx.compose.ui.graphics.Color
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
    Unknown to MapMarkerColor(0xFF000000),
    Unclaimed to MapMarkerColor(0xFFD0021B),
    ClaimedNotStarted to MapMarkerColor(0xFFFAB92E),
    // Assigned
    InProgress to MapMarkerColor(0xFFF0F032),
    PartiallyCompleted to MapMarkerColor(0xFF0054BB),
    NeedsFollowUp to MapMarkerColor(0xFFEA51EB),
    Completed to MapMarkerColor(0xFF82D78C),
    DoneByOthersNhwPc to MapMarkerColor(0xFF0fa355),
    // Unresponsive
    OutOfScopeDu to MapMarkerColor(0xFF787878),
    Incomplete to MapMarkerColor(0xFF1D1D1D),
)
