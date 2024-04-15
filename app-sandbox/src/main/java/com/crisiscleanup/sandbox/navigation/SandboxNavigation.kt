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
import com.crisiscleanup.sandbox.ui.SingleImageRoute

const val ROOT_ROUTE = "root"
private const val CHECKBOXES_ROUTE = "checkboxes"
private const val CHIPS_ROUTE = "chips"
const val SINGLE_IMAGE_ROUTE = "single-image"
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

fun NavController.navigateToSingleImage() {
    this.navigate(SINGLE_IMAGE_ROUTE)
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
        composable(ROOT_ROUTE) {
            RootRoute(navController)
        }

        composable(CHECKBOXES_ROUTE) {
            CheckboxesRoute()
        }

        composable(BOTTOM_NAV_ROUTE) {
            BottomNavRoute()
        }

        composable(CHIPS_ROUTE) {
            ChipsRoute()
        }

        composable(SINGLE_IMAGE_ROUTE) {
            SingleImageRoute()
        }
    }
}
