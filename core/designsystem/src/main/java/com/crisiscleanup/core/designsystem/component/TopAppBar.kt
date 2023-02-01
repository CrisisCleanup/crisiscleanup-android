@file:OptIn(ExperimentalMaterial3Api::class)

package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisCleanupTopAppBar(
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
    isCenterAlign: Boolean = true,
    navIcon: ImageVector? = null,
    navContentDescription: String? = null,
    navIconPadding: Dp = 16.dp,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    onNavigationClick: (() -> Unit)? = null,
    onActionClick: () -> Unit = {},
) {
    val titleContent = @Composable {
        Text(text = stringResource(id = titleResId))
    }
    val navigationContent: (@Composable (() -> Unit)) =
        @Composable {
            if (navIcon != null) {
                if (onNavigationClick == null) {
                    Icon(
                        modifier = Modifier.padding(navIconPadding),
                        imageVector = navIcon,
                        contentDescription = navContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    IconButton(onClick = onNavigationClick) {
                        Icon(
                            imageVector = navIcon,
                            contentDescription = navContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (actionIcon == null) {
        @Composable {}
    } else {
        @Composable {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    if (isCenterAlign) {
        CenterAlignedTopAppBar(
            title = titleContent,
            navigationIcon = navigationContent,
            actions = actionsContent,
            colors = colors,
            modifier = modifier
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationContent,
            actions = actionsContent,
            colors = colors,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttentionBadge(
    addBadge: Boolean,
    padding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    if (addBadge) {
        BadgedBox(
            // TODO Padding around the badge not the content
            modifier = Modifier.padding(padding),
            badge = {
                Badge {
                    Text("!")
                }
            },
        ) {
            content()
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarDefault(
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int = 0,
    title: String = "",
    navIcon: ImageVector? = null,
    navContentDescription: String? = null,
    navIconPadding: Dp = 16.dp,
    actionIcon: ImageVector,
    @StringRes actionResId: Int,
    isActionAttention: Boolean = false,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    profilePictureUri: String = "",
    onNavigationClick: (() -> Unit)? = null,
    onActionClick: () -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
) {
    val barTitle: (@Composable () -> Unit) = titleContent ?: @Composable {
        val text = if (titleResId == 0) title else stringResource(titleResId)
        Text(modifier = modifier, text = text)
    }
    val navigationContent: (@Composable (() -> Unit)) =
        @Composable {
            if (navIcon != null) {
                if (onNavigationClick == null) {
                    Icon(
                        modifier = Modifier.padding(navIconPadding),
                        imageVector = navIcon,
                        contentDescription = navContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    IconButton(onClick = onNavigationClick) {
                        Icon(
                            imageVector = navIcon,
                            contentDescription = navContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = @Composable {
        IconButton(onClick = onActionClick) {
            if (profilePictureUri.isEmpty()) {
                AttentionBadge(isActionAttention) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = stringResource(actionResId),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                AttentionBadge(isActionAttention, 8.dp) {
                    val fallbackPainter = rememberVectorPainter(actionIcon)
                    AsyncImage(
                        model = profilePictureUri,
                        contentDescription = stringResource(actionResId),
                        fallback = fallbackPainter,
                    )
                }
            }
        }
    }

    TopAppBar(
        title = barTitle,
        navigationIcon = navigationContent,
        actions = actionsContent,
        colors = colors,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("center-align")
@Composable
private fun CrisisCleanupTopCenterAppBarPreview() {
    CrisisCleanupTopAppBar(
        titleResId = android.R.string.untitled,
        navIcon = CrisisCleanupIcons.Search,
        navContentDescription = "Nav",
        actionIcon = CrisisCleanupIcons.MoreVert,
        actionIconContentDescription = "Action"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("not-center-align")
@Composable
private fun CrisisCleanupTopAppBarPreview() {
    CrisisCleanupTopAppBar(
        isCenterAlign = false,
        navIcon = CrisisCleanupIcons.Search,
        titleResId = android.R.string.untitled,
        actionIcon = CrisisCleanupIcons.MoreVert,
        actionIconContentDescription = "Action"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("attention-over-icon")
@Composable
private fun CrisisCleanupTopAppBarEndPreview() {
    TopAppBarDefault(
        titleResId = android.R.string.untitled,
        actionIcon = CrisisCleanupIcons.MoreVert,
        actionResId = android.R.string.search_go,
        profilePictureUri = "",
        isActionAttention = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("attention-over-image")
@Composable
private fun CrisisCleanupTopAppBarImagePreview() {
    TopAppBarDefault(
        title = "with icon",
        navIcon = CrisisCleanupIcons.Search,
        actionIcon = CrisisCleanupIcons.MoreVert,
        actionResId = android.R.string.search_go,
        profilePictureUri = "https://avatars.dicebear.com/api/bottts/Demo User.svg",
        isActionAttention = true,
    )
}
