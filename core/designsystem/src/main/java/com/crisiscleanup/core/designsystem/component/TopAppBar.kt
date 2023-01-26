@file:OptIn(ExperimentalMaterial3Api::class)

package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisCleanupTopAppBar(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(id = titleRes)) },
        navigationIcon = {
            if (navigationIcon != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = navigationIconContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            if (actionIcon != null) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionIconContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = colors,
        modifier = modifier
    )
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
    @StringRes titleRes: Int,
    actionIcon: ImageVector,
    @StringRes actionResId: Int,
    isActionAttention: Boolean = false,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    profilePictureUri: String = "",
    onActionClick: () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(titleRes)) },
        actions = {
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
        },
        colors = colors,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarSearchCases(
    modifier: Modifier = Modifier,
    q: () -> String = { "" },
    onQueryChange: (String) -> Unit = {},
    // TODO Use constant for padding
    padding: Dp = 8.dp,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    hasFocus: Boolean = true,
    onSearch: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(windowInsets)
            // clip after padding so we don't show the title over the inset area
            .clipToBounds(),
    ) {
        // TODO Better alignment
        OutlinedClearableTextField(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = padding),
            labelResId = 0,
            value = q(),
            onValueChange = onQueryChange,
            enabled = true,
            isError = false,
            hasFocus = hasFocus,
            onSearch = onSearch,
            imeAction = ImeAction.Search,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("end-icons")
@Composable
private fun CrisisCleanupTopAppBarPreview() {
    CrisisCleanupTopAppBar(
        titleRes = android.R.string.untitled,
        navigationIcon = CrisisCleanupIcons.Search,
        navigationIconContentDescription = "Nav",
        actionIcon = CrisisCleanupIcons.MoreVert,
        actionIconContentDescription = "Action"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("end-icon")
@Composable
private fun CrisisCleanupTopAppBarEndPreview() {
    TopAppBarDefault(
        titleRes = android.R.string.untitled,
        actionIcon = CrisisCleanupIcons.MoreVert,
        actionResId = android.R.string.search_go,
        profilePictureUri = "",
        isActionAttention = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("end-icon")
@Composable
private fun CrisisCleanupTopAppBarImagePreview() {
    TopAppBarDefault(
        titleRes = android.R.string.untitled,
        actionIcon = CrisisCleanupIcons.MoreVert,
        actionResId = android.R.string.search_go,
        profilePictureUri = "https://avatars.dicebear.com/api/bottts/Demo User.svg",
        isActionAttention = true,
    )
}

@Preview
@Composable
private fun TopAppBarSearchCasesPreview() {
    TopAppBarSearchCases(
        q = { "searching" },
    )
}
