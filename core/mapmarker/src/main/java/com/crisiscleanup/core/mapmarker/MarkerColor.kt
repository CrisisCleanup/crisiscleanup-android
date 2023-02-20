package com.crisiscleanup.core.mapmarker

import androidx.compose.ui.graphics.Color
import com.crisiscleanup.core.model.data.CaseStatus.*

data class MapMarkerColor(
    val fillInt: Int,
    val strokeInt: Int,
    val fill: Color = Color(fillInt.toLong() or 0xFF000000),
    val stroke: Color = Color(strokeInt.toLong() or 0xFF000000),
)

internal val mapMarkerColors = mapOf(
    Unknown to MapMarkerColor(0x000000, 0xFFFFFF),
    Unclaimed to MapMarkerColor(0xD0021B, 0xE30001),
    ClaimedNotStarted to MapMarkerColor(0xFAB92E, 0xF79820),
    InProgress to MapMarkerColor(0xF0F032, 0x85863F),
    PartiallyCompleted to MapMarkerColor(0x0054BB, 0x0054BB),
    NeedsFollowUp to MapMarkerColor(0xEA51EB, 0xE018E1),
    Completed to MapMarkerColor(0x82D78C, 0x51AC7C),
    DoneByOthersNhwPc to MapMarkerColor(0x0fa355, 0x0fa355),
    OutOfScopeDu to MapMarkerColor(0x787878, 0x5d5d5d),
    Incomplete to MapMarkerColor(0x1d1d1d, 0x1d1d1d),
)
