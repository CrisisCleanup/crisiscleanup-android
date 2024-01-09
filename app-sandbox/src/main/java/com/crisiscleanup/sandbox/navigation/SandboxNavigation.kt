package com.crisiscleanup.sandbox.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.crisiscleanup.sandbox.RootRoute
import com.crisiscleanup.sandbox.ui.CheckboxesRoute

const val rootRoute = "root"
private const val checkboxesRoute = "checkboxes"

fun NavController.navigateToCheckboxes() {
    this.navigate(checkboxesRoute)
}

@Composable
fun SandboxNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = rootRoute,
    ) {
        composable(route = rootRoute) {
            RootRoute(navController)
        }

        composable(route = checkboxesRoute) {
            CheckboxesRoute()
        }
    }
}
