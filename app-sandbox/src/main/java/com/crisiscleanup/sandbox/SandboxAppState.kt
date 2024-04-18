package com.crisiscleanup.sandbox

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crisiscleanup.sandbox.navigation.SINGLE_IMAGE_ROUTE
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberAppState(
    windowSizeClass: WindowSizeClass,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
): SandboxAppState {
    return remember(navController, coroutineScope, windowSizeClass) {
        SandboxAppState(navController, coroutineScope, windowSizeClass)
    }
}

@Stable
class SandboxAppState(
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
    val windowSizeClass: WindowSizeClass,
) {
    fun onBack() {
        navController.popBackStack()
    }

    private val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val isFullscreenRoute: Boolean
        @Composable get() {
            val route = currentDestination?.route ?: ""
            return route.startsWith(SINGLE_IMAGE_ROUTE)
        }
}
