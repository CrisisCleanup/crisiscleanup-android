package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.avatarAttentionColor
import com.crisiscleanup.core.designsystem.theme.avatarStandardColor
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor

private val titleStyle: TextStyle
    @Composable
    get() = LocalFontStyles.current.header3

@Composable
fun TruncatedAppBarText(
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int = 0,
    title: String = "",
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxWidth = screenWidth.times(0.65f)
    val text = if (titleResId == 0) title else stringResource(titleResId)
    Text(
        text,
        modifier = modifier.widthIn(max = maxWidth),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = titleStyle,
    )
}

@Composable
private fun IconButton(
    onClick: () -> Unit,
    iconImage: ImageVector,
    contentDescription: String? = null,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = iconImage,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StaticDynamicIcon(
    imageIcon: ImageVector,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    staticIconPadding: Dp = 16.dp,
) {
    if (onClick == null) {
        Icon(
            modifier = Modifier.padding(staticIconPadding),
            imageVector = imageIcon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        IconButton(onClick, imageIcon, contentDescription)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisCleanupTopAppBar(
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int = 0,
    title: String = "",
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
        TruncatedAppBarText(modifier, titleResId, title)
    }
    val navigationContent: (@Composable (() -> Unit)) =
        @Composable {
            navIcon?.let {
                StaticDynamicIcon(
                    it,
                    onNavigationClick,
                    navContentDescription,
                    navIconPadding,
                )
            }
        }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (actionIcon == null) {
        @Composable {}
    } else {
        @Composable { IconButton(onActionClick, actionIcon, actionIconContentDescription) }
    }
    if (isCenterAlign) {
        CenterAlignedTopAppBar(
            title = titleContent,
            navigationIcon = navigationContent,
            actions = actionsContent,
            colors = colors,
            modifier = modifier,
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationContent,
            actions = actionsContent,
            colors = colors,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarBackCancel(
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int = 0,
    title: String = "",
    onBack: () -> Unit = {},
    onCancel: () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
) {
    val titleContent = @Composable { TruncatedAppBarText(modifier, titleResId, title) }
    val navigationContent = @Composable { TopBarBackAction(action = onBack) }
    val actionsContent: (@Composable (RowScope.() -> Unit)) =
        @Composable {
            Text(
                LocalAppTranslator.current("actions.cancel"),
                // TODO Style, height of app bar
                modifier
                    .clickable(onClick = onCancel)
                    .padding(8.dp),
                style = titleStyle,
            )
        }
    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
        actions = actionsContent,
        colors = colors,
        modifier = modifier,
    )
}

@Composable
private fun TopBarNavAction(
    modifier: Modifier = Modifier,
    action: () -> Unit = {},
    @StringRes textResId: Int = 0,
    text: String = "",
    image: ImageVector? = CrisisCleanupIcons.ArrowBack,
) {
    // TODO Text style and match height of app bar
    Row(
        modifier
            .clickable(onClick = action)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides primaryBlueColor) {
            val actionText = text.ifEmpty { if (textResId != 0) stringResource(textResId) else "" }
            image?.let {
                Icon(
                    imageVector = image,
                    contentDescription = actionText,
                )
            }
            Text(actionText)
        }
    }
}

@Composable
fun TopBarBackAction(
    modifier: Modifier = Modifier,
    action: () -> Unit = {},
) {
    TopBarNavAction(
        modifier,
        action,
        text = LocalAppTranslator.current("actions.back"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarBackAction(
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int = 0,
    title: String = "",
    onAction: () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
) = TopAppBarBackCaretAction(
    modifier,
    titleResId = titleResId,
    title = title,
    onAction = onAction,
    colors = colors,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarCancelAction(
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int = 0,
    title: String = "",
    onAction: () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
) = TopAppBarBackCaretAction(
    modifier,
    "actions.cancel",
    titleResId,
    title,
    onAction,
    colors,
    actionTestTag = "topNavCancelAction",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarBackCaretAction(
    modifier: Modifier = Modifier,
    navigationTranslateKey: String = "actions.back",
    @StringRes titleResId: Int = 0,
    title: String = "",
    onAction: () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    actionTestTag: String = "topNavBackAction",
    actions: @Composable RowScope.() -> Unit = {},
) {
    val titleContent = @Composable {
        TruncatedAppBarText(modifier, titleResId, title)
    }
    val navigationContent: (@Composable (() -> Unit)) =
        @Composable {
            TopBarNavAction(
                modifier.testTag(actionTestTag),
                onAction,
                text = LocalAppTranslator.current(navigationTranslateKey),
            )
        }
    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
        actions = actions,
        colors = colors,
        modifier = modifier,
    )
}

@Composable
private fun AvatarAttentionBadge(
    isAlert: Boolean,
    content: @Composable () -> Unit,
) {
    BadgedBox(
        badge = {
            Badge(
                // TODO Common/relative dimensions
                Modifier
                    .size(16.dp, 16.dp)
                    .offset((-4).dp, 32.dp),
                containerColor = if (isAlert) avatarAttentionColor else avatarStandardColor,
            )
        },
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarDefault(
    modifier: Modifier = Modifier,
    accountToggleModifier: Modifier = Modifier,
    @StringRes titleResId: Int = 0,
    title: String = "",
    navIcon: ImageVector? = null,
    navContentDescription: String? = null,
    navIconPadding: Dp = 16.dp,
    actionIcon: ImageVector = CrisisCleanupIcons.QuestionMark,
    actionText: String = "",
    isActionAttention: Boolean = false,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    profilePictureUri: String = "",
    onNavigationClick: (() -> Unit)? = null,
    onActionClick: () -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
) {
    val barTitle: (@Composable () -> Unit) = titleContent ?: @Composable {
        TruncatedAppBarText(modifier, titleResId, title)
    }
    val navigationContent: (@Composable (() -> Unit)) =
        @Composable {
            navIcon?.let {
                StaticDynamicIcon(
                    navIcon,
                    onNavigationClick,
                    navContentDescription,
                    navIconPadding,
                )
            }
        }
    val actionsContent: (@Composable (RowScope.() -> Unit)) = @Composable {
        AvatarAttentionBadge(isActionAttention) {
            IconButton(
                onClick = onActionClick,
                modifier = accountToggleModifier.testTag("topBarAvatarIconBtn"),
            ) {
                AvatarIcon(
                    profilePictureUri,
                    actionText,
                    fallbackIcon = actionIcon,
                )
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
        actionIconContentDescription = "Action",
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
        actionIconContentDescription = "Action",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("action-bar-back")
@Composable
private fun TopAppBarBackActionPreview() {
    TopAppBarBackAction(
        title = "Top bar",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("attention-over-icon")
@Composable
private fun CrisisCleanupTopAppBarEndPreview() {
    TopAppBarDefault(
        titleResId = android.R.string.untitled,
        actionIcon = CrisisCleanupIcons.MoreVert,
        actionText = "action",
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
        actionText = "action",
        profilePictureUri = "https://profile-picture.svg",
        isActionAttention = true,
    )
}

@Preview
@Composable
private fun TopAppBarBackPreview() {
    TopBarNavAction()
}
