package com.crisiscleanup.feature.menu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.crisiscleanup.feature.menu.MenuRoute

const val menuRoute = "menu_route"

fun NavController.navigateToMenu(navOptions: NavOptions? = null) {
    this.navigate(menuRoute, navOptions)
}

fun NavGraphBuilder.menuScreen() {
    composable(route = menuRoute) {
        MenuRoute()
    }
}
