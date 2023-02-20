package com.crisiscleanup.core.mapmarker

import androidx.compose.ui.graphics.Color
import com.crisiscleanup.core.model.data.CaseStatus.*

data class MapMarkerColor(
    val fill: Color,
    val stroke: Color,
)

private fun makeDotColor(fill: Long, stroke: Long) = MapMarkerColor(
    Color(fill),
    Color(stroke),
)

internal val mapMarkerColors = mapOf(
    Unknown to makeDotColor(0xFF000000, 0xFFFFFFFF),
    Unclaimed to makeDotColor(0xFFD0021B, 0xFFE30001),
    ClaimedNotStarted to makeDotColor(0xFFFAB92E, 0xFFF79820),
    InProgress to makeDotColor(0xFFF0F032, 0xFF85863F),
    PartiallyCompleted to makeDotColor(0xFF0054BB, 0xFF0054BB),
    NeedsFollowUp to makeDotColor(0xFFEA51EB, 0xFFE018E1),
    Completed to makeDotColor(0xFF82D78C, 0xFF51AC7C),
    DoneByOthersNhwPc to makeDotColor(0xFF0fa355, 0xFF0fa355),
    OutOfScopeDu to makeDotColor(0xFF787878, 0xFF5d5d5d),
    Incomplete to makeDotColor(0xFF1d1d1d, 0xFF1d1d1d),
)
