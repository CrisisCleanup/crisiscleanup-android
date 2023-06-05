package com.crisiscleanup.navigation

import androidx.annotation.StringRes
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.icon.Icon
import com.crisiscleanup.core.designsystem.icon.Icon.DrawableResourceIcon
import com.crisiscleanup.core.designsystem.icon.Icon.ImageVectorIcon

enum class TopLevelDestination(
    val selectedIcon: Icon,
    val unselectedIcon: Icon,
    val titleTranslateKey: String,
    @StringRes val titleResId: Int = 0,
) {
    CASES(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Cases),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Cases),
        titleTranslateKey = "casesVue.cases",
    ),
    DASHBOARD(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Dashboard),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Dashboard),
        titleTranslateKey = "nav.dashboard",
    ),
    TEAM(
        selectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Team),
        unselectedIcon = DrawableResourceIcon(CrisisCleanupIcons.Team),
        titleTranslateKey = "userView.team",
    ),
    MENU(
        selectedIcon = ImageVectorIcon(CrisisCleanupIcons.Menu),
        unselectedIcon = ImageVectorIcon(CrisisCleanupIcons.Menu),
        titleTranslateKey = "nav.menu",
        titleResId = com.crisiscleanup.feature.menu.R.string.menu,
    )
}
