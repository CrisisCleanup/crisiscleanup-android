package com.crisiscleanup.core.designsystem.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Dimensions(
    /**
     * Space between element border and edges of screen
     */
    val edgePadding: Dp = 16.dp,
    /**
     * Space between row items
     */
    val itemInnerPaddingHorizontal: Dp = 16.dp,
    /**
     * Space between element border and edges of screen on varying screen sizes
     */
    val edgePaddingFlexible: Dp = 16.dp,
    /**
     * Space between row items on varying screen sizes
     */
    val itemInnerPaddingHorizontalFlexible: Dp = 16.dp,
    val isThinScreenWidth: Boolean = false,
    val isLandscape: Boolean = false,
    val isPortrait: Boolean = true,
    val isListDetailWidth: Boolean = false,
) {
    val itemInnerSpacingHorizontalFlexible: Arrangement.HorizontalOrVertical =
        Arrangement.spacedBy(itemInnerPaddingHorizontalFlexible)
}

val w360Dimensions = Dimensions().copy(
    edgePadding = 8.dp,
    itemInnerPaddingHorizontal = 8.dp,
    edgePaddingFlexible = 1.dp,
    itemInnerPaddingHorizontalFlexible = 1.dp,
    isThinScreenWidth = true,
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }
