package com.crisiscleanup.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize

data class LayoutSizePosition(
    val size: IntSize = IntSize.Zero,
    val position: Offset = Offset.Zero,
)

val LayoutCoordinates.sizePosition: LayoutSizePosition
    get() = LayoutSizePosition(size, positionInRoot())
