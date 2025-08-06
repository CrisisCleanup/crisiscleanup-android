package com.crisiscleanup.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationBar
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationBarItem
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationRail
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationRailItem
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.icon.Icon
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor
import com.crisiscleanup.navigation.TopLevelDestination

@Composable
private fun TopLevelDestination.Icon(isSelected: Boolean, description: String) {
    val icon = if (isSelected) {
        selectedIcon
    } else {
        unselectedIcon
    }
    when (icon) {
        is Icon.ImageVectorIcon -> Icon(
            imageVector = icon.imageVector,
            contentDescription = description,
        )

        is Icon.DrawableResourceIcon -> {
            var tint = LocalContentColor.current
            if (isSelected) {
                tint = Color.White
            }
            Icon(
                painter = painterResource(id = icon.id),
                contentDescription = description,
                tint = tint,
            )
        }
    }
}

@Composable
private fun NavItems(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    isAppUpdateAvailable: Boolean,
    itemContent: @Composable (
        Boolean,
        String,
        Boolean,
        () -> Unit,
        @Composable () -> Unit,
        @Composable () -> Unit,
    ) -> Unit,
) {
    val t = LocalAppTranslator.current
    val translationCount by t.translationCount.collectAsStateWithLifecycle()
    destinations.forEachIndexed { i, destination ->
        val title = remember(translationCount) {
            t(destination.titleTranslateKey)
        }
        val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
        val badgeImage = if (isAppUpdateAvailable && destination == TopLevelDestination.MENU) {
            CrisisCleanupIcons.AppUpdateAvailable
        } else {
            null
        }
        itemContent(
            selected,
            title,
            i == destinations.size - 1,
            { onNavigateToDestination(destination) },
            {
                BadgedView(badgeImage) {
                    destination.Icon(selected, title)
                }
            },
            {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }
}

@Composable
internal fun AppNavigationBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    isAppUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
    isRail: Boolean = false,
) {
    if (isRail) {
        CrisisCleanupNavRail(
            destinations,
            onNavigateToDestination,
            currentDestination,
            isAppUpdateAvailable,
            modifier,
        )
    } else {
        CrisisCleanupBottomBar(
            destinations,
            onNavigateToDestination,
            currentDestination,
            isAppUpdateAvailable,
            modifier,
        )
    }
}

@Composable
internal fun AppNavigationBar(
    appState: CrisisCleanupAppState,
    isAppUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
    isRail: Boolean = false,
) {
    AppNavigationBar(
        appState.topLevelDestinations,
        appState::navigateToTopLevelDestination,
        appState.currentDestination,
        isAppUpdateAvailable = isAppUpdateAvailable,
        modifier,
        isRail,
    )
}

@Composable
private fun BadgedView(
    badgeIcon: ImageVector?,
    content: @Composable () -> Unit,
) {
    if (badgeIcon == null) {
        content()
    } else {
        BadgedBox(
            badge = {
                Badge(
                    Modifier.size(20.dp),
                    containerColor = primaryOrangeColor,
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                    )
                }
            },
        ) {
            content()
        }
    }
}

@Composable
private fun CrisisCleanupNavRail(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    isAppUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    CrisisCleanupNavigationRail(modifier = modifier) {
        Spacer(Modifier.weight(1f))
        NavItems(
            destinations = destinations,
            onNavigateToDestination = onNavigateToDestination,
            currentDestination = currentDestination,
            isAppUpdateAvailable = isAppUpdateAvailable,
        ) {
                isSelected: Boolean,
                title: String,
                isLastItem: Boolean,
                onClick: () -> Unit,
                iconContent: @Composable () -> Unit,
                labelContent: @Composable () -> Unit,
            ->
            CrisisCleanupNavigationRailItem(
                selected = isSelected,
                onClick = onClick,
                icon = iconContent,
                label = labelContent,
                modifier = Modifier.weight(1f)
                    .testTag("navItem_$title"),
            )

            if (!isLastItem) {
                HorizontalDivider(
                    Modifier.sizeIn(maxWidth = 36.dp),
                    thickness = 1.dp,
                    color = CrisisCleanupNavigationDefaults.navigationSelectedItemColor()
                        .disabledAlpha(),
                )
            }
        }
    }
}

@Composable
private fun CrisisCleanupBottomBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    isAppUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    CrisisCleanupNavigationBar(modifier = modifier) {
        NavItems(
            destinations = destinations,
            onNavigateToDestination = onNavigateToDestination,
            currentDestination = currentDestination,
            isAppUpdateAvailable = isAppUpdateAvailable,
        ) {
                isSelected: Boolean,
                title: String,
                isLastItem: Boolean,
                onClick: () -> Unit,
                iconContent: @Composable () -> Unit,
                labelContent: @Composable () -> Unit,
            ->
            CrisisCleanupNavigationBarItem(
                selected = isSelected,
                onClick = onClick,
                icon = iconContent,
                label = labelContent,
                modifier = Modifier.testTag("navItem_$title"),
            )

            if (!isLastItem) {
                VerticalDivider(
                    Modifier.sizeIn(maxHeight = 36.dp),
                    thickness = 1.dp,
                    color = CrisisCleanupNavigationDefaults.navigationSelectedItemColor()
                        .disabledAlpha(),
                )
            }
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination) =
    this?.hierarchy?.any {
        it.route?.contains(destination.name, true) ?: false
    } ?: false
