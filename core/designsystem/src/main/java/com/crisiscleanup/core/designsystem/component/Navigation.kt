package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

/**
 * App navigation bar item with icon and label content slots. Wraps Material 3
 * [NavigationBarItem].
 *
 * @param selected Whether this item is selected.
 * @param onClick The callback to be invoked when this item is selected.
 * @param icon The item icon content.
 * @param modifier Modifier to be applied to this item.
 * @param selectedIcon The item icon content when selected.
 * @param enabled controls the enabled state of this item. When `false`, this item will not be
 * clickable and will appear disabled to accessibility services.
 * @param label The item text label content.
 * @param alwaysShowLabel Whether to always show the label for this item. If false, the label will
 * only be shown when this item is selected.
 */

@Composable
fun RowScope.CrisisCleanupNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectedIcon: @Composable () -> Unit = icon,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
) {
    val selectedColor = CrisisCleanupNavigationDefaults.navigationSelectedItemColor()
    val unselectedColor = selectedColor.copy(0.5f)
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = selectedColor,
            unselectedIconColor = unselectedColor,
            selectedTextColor = selectedColor,
            unselectedTextColor = unselectedColor,
            indicatorColor = CrisisCleanupNavigationDefaults.navigationIndicatorColor(),
        ),
    )
}

/**
 * App navigation bar with content slot. Wraps Material 3 [NavigationBar].
 *
 * @param modifier Modifier to be applied to the navigation bar.
 * @param content Destinations inside the navigation bar. This should contain multiple
 * [NavigationBarItem]s.
 */
@Composable
fun CrisisCleanupNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        modifier = modifier.testTag("appNavBar"),
        containerColor = CrisisCleanupNavigationDefaults.navigationContainerColor(),
        contentColor = CrisisCleanupNavigationDefaults.navigationContentColor(),
        tonalElevation = 0.dp,
        content = content,
    )
}

/**
 * App navigation rail item with icon and label content slots. Wraps Material 3
 * [NavigationRailItem].
 *
 * @param selected Whether this item is selected.
 * @param onClick The callback to be invoked when this item is selected.
 * @param icon The item icon content.
 * @param modifier Modifier to be applied to this item.
 * @param selectedIcon The item icon content when selected.
 * @param enabled controls the enabled state of this item. When `false`, this item will not be
 * clickable and will appear disabled to accessibility services.
 * @param label The item text label content.
 * @param alwaysShowLabel Whether to always show the label for this item. If false, the label will
 * only be shown when this item is selected.
 */
@Composable
fun CrisisCleanupNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectedIcon: @Composable () -> Unit = icon,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = CrisisCleanupNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = CrisisCleanupNavigationDefaults.navigationContentColor(),
            selectedTextColor = CrisisCleanupNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = CrisisCleanupNavigationDefaults.navigationContentColor(),
            indicatorColor = CrisisCleanupNavigationDefaults.navigationIndicatorColor(),
        ),
    )
}

/**
 * App navigation rail with header and content slots. Wraps Material 3 [NavigationRail].
 *
 * @param modifier Modifier to be applied to the navigation rail.
 * @param header Optional header that may hold a floating action button or a logo.
 * @param content Destinations inside the navigation rail. This should contain multiple
 * [NavigationRailItem]s.
 */
@Composable
fun CrisisCleanupNavigationRail(
    modifier: Modifier = Modifier,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = CrisisCleanupNavigationDefaults.navigationContainerColor(),
        contentColor = CrisisCleanupNavigationDefaults.navigationContentColor(),
        header = header,
        content = content,
    )
}

/**
 * Navigation default colors.
 */
object CrisisCleanupNavigationDefaults {
    private val containerColor = com.crisiscleanup.core.designsystem.theme.navigationContainerColor

    @Composable
    fun navigationContainerColor() = containerColor

    @Composable
    fun navigationContentColor() = Color(0xFFB8B8B8)

    @Composable
    fun navigationSelectedItemColor() = Color(0xFFF6F8F9)

    @Composable
    fun navigationIndicatorColor() = containerColor
}

@Preview
@Composable
private fun CrisisCleanupNavigationBarPreview() {
    CrisisCleanupNavigationBar {
        CrisisCleanupNavigationBarItem(
            selected = false,
            onClick = {},
            icon = {
                Icon(
                    imageVector = CrisisCleanupIcons.Edit,
                    contentDescription = null,
                )
            },
            label = { Text("eay") },
        )
        CrisisCleanupNavigationBarItem(
            selected = true,
            onClick = {},
            icon = {
                Icon(
                    imageVector = CrisisCleanupIcons.Visibility,
                    contentDescription = null,
                )
            },
            label = { Text("baa") },
        )
    }
}

@Preview
@Composable
private fun CrisisCleanupNavigationRailPreview() {
    CrisisCleanupNavigationRail {
        CrisisCleanupNavigationRailItem(
            selected = false,
            onClick = {},
            icon = {
                Icon(
                    imageVector = CrisisCleanupIcons.Edit,
                    contentDescription = null,
                )
            },
            label = { Text("eay") },
        )
        CrisisCleanupNavigationRailItem(
            selected = true,
            onClick = {},
            icon = {
                Icon(
                    imageVector = CrisisCleanupIcons.Visibility,
                    contentDescription = null,
                )
            },
            label = { Text("baa") },
        )
    }
}
