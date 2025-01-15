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
import com.crisiscleanup.sandbox.ui.MapMarkersView
import com.crisiscleanup.sandbox.ui.MultiImageRoute
import com.crisiscleanup.sandbox.ui.SingleImageRoute

const val ROOT_ROUTE = "root"
private const val CHECKBOXES_ROUTE = "checkboxes"
private const val CHIPS_ROUTE = "chips"
private const val BOTTOM_NAV_ROUTE = "bottom-nav"
const val SINGLE_IMAGE_ROUTE = "single-image"
const val MULTI_IMAGE_ROUTE = "multi-image"
const val MAP_MARKERS_ROUTE = "map-markers"

fun NavController.navigateToBottomNav() {
    navigate(BOTTOM_NAV_ROUTE)
}

fun NavController.navigateToCheckboxes() {
    navigate(CHECKBOXES_ROUTE)
}

fun NavController.navigateToChips() {
    navigate(CHIPS_ROUTE)
}

fun NavController.navigateToSingleImage() {
    navigate(SINGLE_IMAGE_ROUTE)
}

fun NavController.navigateToMultiImage() {
    navigate(MULTI_IMAGE_ROUTE)
}

fun NavController.navigateToMapMarkers() {
    navigate(MAP_MARKERS_ROUTE)
}

@Composable
fun SandboxNavHost(
    navController: NavHostController,
    onBack: () -> Unit,
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
            SingleImageRoute(onBack)
        }

        composable(MULTI_IMAGE_ROUTE) {
            MultiImageRoute(onBack)
        }

        composable(MAP_MARKERS_ROUTE) {
            MapMarkersView()
        }
    }
}
