package com.crisiscleanup.sandbox.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.crisiscleanup.sandbox.RootRoute
import com.crisiscleanup.sandbox.ui.BottomNavRoute
import com.crisiscleanup.sandbox.ui.CheckboxesRoute
import com.crisiscleanup.sandbox.ui.ChipsRoute

const val ROOT_ROUTE = "root"
private const val CHECKBOXES_ROUTE = "checkboxes"
const val CHIPS_ROUTE = "chips"
private const val BOTTOM_NAV_ROUTE = "bottom-nav"

fun NavController.navigateToBottomNav() {
    this.navigate(BOTTOM_NAV_ROUTE)
}

fun NavController.navigateToCheckboxes() {
    this.navigate(CHECKBOXES_ROUTE)
}

fun NavController.navigateToChips() {
    this.navigate(CHIPS_ROUTE)
}

@Composable
fun SandboxNavHost(
    navController: NavHostController,
    startDestination: String = ROOT_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(route = ROOT_ROUTE) {
            RootRoute(navController)
        }

        composable(route = CHECKBOXES_ROUTE) {
            CheckboxesRoute()
        }

        composable(route = BOTTOM_NAV_ROUTE) {
            BottomNavRoute()
        }

        composable(route = CHIPS_ROUTE) {
            ChipsRoute()
        }
    }
}
