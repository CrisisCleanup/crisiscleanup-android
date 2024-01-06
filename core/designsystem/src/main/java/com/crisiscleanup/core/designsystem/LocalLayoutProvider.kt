package com.crisiscleanup.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class LayoutProvider(
    val isBottomNav: Boolean = false,
)

val LocalLayoutProvider = staticCompositionLocalOf { LayoutProvider() }
