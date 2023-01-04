package com.crisiscleanup.navigation

import com.crisiscleanup.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.designsystem.icon.Icon
import com.crisiscleanup.designsystem.icon.Icon.DrawableResourceIcon
import com.crisiscleanup.feature.cases.R as casesR
import com.crisiscleanup.feature.dashboard.R as dashboardR
import com.crisiscleanup.feature.team.R as teamR

/**
 * Type for the top level destinations in the application. Each of these destinations
 * can contain one or more screens (based on the window size). Navigation from one screen to the
 * next within a single destination will be handled directly in composables.
 */
enum class TopLevelDestination(
    val selectedIcon: Icon,
    val unselectedIcon: Icon,
    val iconTextId: Int,
    val titleTextId: Int
) {
    CASES(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Cases),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Cases),
        iconTextId = casesR.string.cases,
        titleTextId = casesR.string.cases
    ),
    DASHBOARD(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Dashboard),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Dashboard),
        iconTextId = dashboardR.string.dashboard,
        titleTextId = dashboardR.string.dashboard,
    ),
    TEAM(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Team),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Team),
        iconTextId = teamR.string.team,
        titleTextId = teamR.string.team,
    ),
}
