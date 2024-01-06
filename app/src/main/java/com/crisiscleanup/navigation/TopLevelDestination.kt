package com.crisiscleanup.navigation

import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.icon.Icon
import com.crisiscleanup.core.designsystem.icon.Icon.DrawableResourceIcon
import com.crisiscleanup.core.designsystem.icon.Icon.ImageVectorIcon

enum class TopLevelDestination(
    val selectedIcon: Icon,
    val unselectedIcon: Icon,
    val titleTranslateKey: String,
) {
    // TODO Icon color should change selected vs unselected.
    CASES(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Cases),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Cases),
        titleTranslateKey = "nav.work",
    ),
    DASHBOARD(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Dashboard),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Dashboard),
        titleTranslateKey = "nav.dashboard",
    ),
    TEAM(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Team),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Team),
        titleTranslateKey = "nav.organization_teams",
    ),
    MENU(
        selectedIcon = ImageVectorIcon(CrisisCleanupIcons.Menu),
        unselectedIcon = ImageVectorIcon(CrisisCleanupIcons.Menu),
        titleTranslateKey = "nav.menu",
    ),
}
