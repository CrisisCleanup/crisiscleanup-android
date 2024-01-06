package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import com.crisiscleanup.core.designsystem.theme.cardContainerColor
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.R

private val existingCaseActions = listOf(
    IconTextAction(
        iconResId = R.drawable.ic_share_small,
        translationKey = "actions.share",
    ),
    IconTextAction(
        iconResId = R.drawable.ic_flag_small,
        translationKey = "nav.flag",
    ),
    IconTextAction(
        iconResId = R.drawable.ic_history_small,
        translationKey = "actions.history",
    ),
    IconTextAction(
        iconResId = R.drawable.ic_edit_underscore_small,
        translationKey = "actions.edit",
    ),
)

@Composable
private fun NavItems(
    worksite: Worksite,
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
    onCaseFlags: () -> Unit = {},
    onCaseShare: () -> Unit = {},
    onCaseHistory: () -> Unit = {},
    itemContent: @Composable (
        String,
        () -> Unit,
        @Composable () -> Unit,
        @Composable () -> Unit,
    ) -> Unit,
) {
    existingCaseActions.forEachIndexed { index, action ->
        var label = LocalAppTranslator.current(action.translationKey)
        if (action.translationKey.isNotBlank()) {
            if (label == action.translationKey && action.textResId != 0) {
                label = stringResource(action.textResId)
            }
        }
        if (label.isBlank() && action.textResId != 0) {
            label = stringResource(action.textResId)
        }

        itemContent(
            label,
            {
                when (index) {
                    0 -> onCaseShare()
                    1 -> onCaseFlags()
                    2 -> onCaseHistory()
                    3 -> onFullEdit(
                        ExistingWorksiteIdentifier(
                            worksite.incidentId,
                            worksite.id,
                        ),
                    )
                }
            },
            {
                if (action.iconResId != 0) {
                    Icon(
                        painter = painterResource(action.iconResId),
                        contentDescription = label,
                    )
                } else if (action.imageVector != null) {
                    Icon(
                        imageVector = action.imageVector,
                        contentDescription = label,
                    )
                }
            },
            {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }
}

// TODO Icon color is not correct on first screen load
//      Is correct when navigates back
private fun navItemColor(isEditable: Boolean): Color {
    var contentColor = Color.Black
    if (!isEditable) {
        contentColor = contentColor.disabledAlpha()
    }
    return contentColor
}

@Composable
internal fun ViewCaseNav(
    worksite: Worksite,
    isEditable: Boolean,
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
    onCaseFlags: () -> Unit = {},
    onCaseShare: () -> Unit = {},
    onCaseHistory: () -> Unit = {},
    isBottomNav: Boolean = false,
) {
    if (isBottomNav) {
        BottomNav(
            worksite,
            isEditable,
            onFullEdit,
            onCaseFlags,
            onCaseShare,
            onCaseHistory,
        )
    } else {
        RailNav(
            worksite,
            isEditable,
            onFullEdit,
            onCaseFlags,
            onCaseShare,
            onCaseHistory,
        )
    }
}

@Composable
private fun RailNav(
    worksite: Worksite,
    isEditable: Boolean,
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
    onCaseFlags: () -> Unit = {},
    onCaseShare: () -> Unit = {},
    onCaseHistory: () -> Unit = {},
) {
    val contentColor = navItemColor(isEditable)
    NavigationRail(
        containerColor = cardContainerColor,
        contentColor = contentColor,
    ) {
        Spacer(Modifier.weight(1f))
        NavItems(
            worksite,
            onFullEdit = onFullEdit,
            onCaseFlags = onCaseFlags,
            onCaseShare = onCaseShare,
            onCaseHistory = onCaseHistory,
        ) {
                label: String,
                onClick: () -> Unit,
                iconContent: @Composable () -> Unit,
                labelContent: @Composable () -> Unit,
            ->
            NavigationRailItem(
                modifier = Modifier.testTag("editCaseNavItem_$label"),
                selected = false,
                onClick = onClick,
                icon = iconContent,
                label = labelContent,
                colors = NavigationRailItemDefaults.colors(
                    unselectedIconColor = contentColor,
                    unselectedTextColor = contentColor,
                    indicatorColor = CrisisCleanupNavigationDefaults.navigationIndicatorColor(),
                ),
                enabled = isEditable,
            )
        }
    }
}

@Composable
private fun BottomNav(
    worksite: Worksite,
    isEditable: Boolean,
    onFullEdit: (ExistingWorksiteIdentifier) -> Unit = {},
    onCaseFlags: () -> Unit = {},
    onCaseShare: () -> Unit = {},
    onCaseHistory: () -> Unit = {},
) {
    val contentColor = navItemColor(isEditable)
    NavigationBar(
        containerColor = cardContainerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
    ) {
        NavItems(
            worksite,
            onFullEdit = onFullEdit,
            onCaseFlags = onCaseFlags,
            onCaseShare = onCaseShare,
            onCaseHistory = onCaseHistory,
        ) {
                label: String,
                onClick: () -> Unit,
                iconContent: @Composable () -> Unit,
                labelContent: @Composable () -> Unit,
            ->
            NavigationBarItem(
                modifier = Modifier.testTag("editCaseNavItem_$label"),
                selected = false,
                onClick = onClick,
                icon = iconContent,
                label = labelContent,
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = contentColor,
                    unselectedTextColor = contentColor,
                    indicatorColor = CrisisCleanupNavigationDefaults.navigationIndicatorColor(),
                ),
                enabled = isEditable,
            )
        }
    }
}
