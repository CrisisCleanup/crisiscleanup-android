package com.crisiscleanup.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppLayoutArea(
    private val snackbarHostState: SnackbarHostState? = null,
) {
    val isBottomSnackbarVisible: Boolean
        get() = snackbarHostState?.currentSnackbarData != null

    // TODO Common dimensions or query from system
    val bottomSnackbarPadding: Dp
        get() = if (isBottomSnackbarVisible) 56.dp else 0.dp
}

val LocalAppLayout = staticCompositionLocalOf { AppLayoutArea() }
