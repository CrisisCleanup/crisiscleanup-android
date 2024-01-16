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

const val rootRoute = "root"
private const val checkboxesRoute = "checkboxes"
const val chipsRoute = "chips"
private const val bottomNavRoute = "bottom-nav"

fun NavController.navigateToBottomNav() {
    this.navigate(bottomNavRoute)
}

fun NavController.navigateToCheckboxes() {
    this.navigate(checkboxesRoute)
}

fun NavController.navigateToChips() {
    this.navigate(chipsRoute)
}

@Composable
fun SandboxNavHost(
    navController: NavHostController,
    startDestination: String = rootRoute,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(route = rootRoute) {
            RootRoute(navController)
        }

        composable(route = checkboxesRoute) {
            CheckboxesRoute()
        }

        composable(route = bottomNavRoute) {
            BottomNavRoute()
        }

        composable(route = chipsRoute) {
            ChipsRoute()
        }
    }
}
