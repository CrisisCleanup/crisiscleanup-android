package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.HeaderSubTitle
import com.crisiscleanup.core.designsystem.component.HeaderTitle
import com.crisiscleanup.core.designsystem.component.TopBarBackAction
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.primaryRedColor
import com.crisiscleanup.feature.caseeditor.R

private fun getTopIconActionColor(
    isActive: Boolean,
    isEditable: Boolean,
): Color {
    var tint = if (isActive) {
        primaryRedColor
    } else {
        neutralIconColor
    }
    if (!isEditable) {
        tint = tint.disabledAlpha()
    }
    return tint
}

@Composable
private fun HeaderActions(
    isEditable: Boolean,
    isHighPriority: Boolean,
    isFavorite: Boolean,
    toggleHighPriority: () -> Unit,
    toggleFavorite: () -> Unit,
) {
    val t = LocalAppTranslator.current
    val highPriorityTranslateKey = if (isHighPriority) {
        "actions.unmark_high_priority"
    } else {
        "flag.flag_high_priority"
    }
    val highPriorityTint = getTopIconActionColor(isHighPriority, isEditable)
    CrisisCleanupIconButton(
        iconResId = R.drawable.ic_important_filled,
        contentDescription = t(highPriorityTranslateKey),
        onClick = toggleHighPriority,
        enabled = isEditable,
        tint = highPriorityTint,
        modifier = Modifier.testTag("editCaseHighPriorityToggleBtn"),
    )

    val iconResId = if (isFavorite) {
        R.drawable.ic_heart_filled
    } else {
        R.drawable.ic_heart_outline
    }
    val favoriteDescription = if (isFavorite) {
        t("actions.not_member_of_my_org")
    } else {
        t("actions.member_of_my_org")
    }
    val favoriteTint = getTopIconActionColor(isFavorite, isEditable)
    CrisisCleanupIconButton(
        iconResId = iconResId,
        contentDescription = favoriteDescription,
        onClick = toggleFavorite,
        enabled = isEditable,
        tint = favoriteTint,
        modifier = Modifier.testTag("editCaseFavoriteToggleBtn"),
    )
}

@Composable
internal fun ColumnScope.ViewCaseHeader(
    title: String,
    subTitle: String = "",
    updatedAtText: String,
    isFavorite: Boolean = false,
    isHighPriority: Boolean = false,
    hideHeaderActions: Boolean = false,
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    toggleFavorite: () -> Unit = {},
    toggleHighPriority: () -> Unit = {},
    isEditable: Boolean = false,
    onCaseLongPress: () -> Unit = {},
    isTopBar: Boolean = false,
) {
    if (isTopBar) {
        TopBarHeader(
            title,
            subTitle,
            isFavorite = isFavorite,
            isHighPriority = isHighPriority,
            hideHeaderActions = hideHeaderActions,
            onBack,
            isLoading,
            toggleFavorite = toggleFavorite,
            toggleHighPriority = toggleHighPriority,
            isEditable,
            onCaseLongPress,
        )
    } else {
        SideHeader(
            title,
            subTitle,
            updatedAtText,
            isFavorite = isFavorite,
            isHighPriority = isHighPriority,
            hideHeaderActions = hideHeaderActions,
            onBack,
            isLoading,
            toggleFavorite = toggleFavorite,
            toggleHighPriority = toggleHighPriority,
            isEditable,
            onCaseLongPress,
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
private fun TopBarHeader(
    title: String,
    subTitle: String = "",
    isFavorite: Boolean = false,
    isHighPriority: Boolean = false,
    hideHeaderActions: Boolean = false,
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    toggleFavorite: () -> Unit = {},
    toggleHighPriority: () -> Unit = {},
    isEditable: Boolean = false,
    onCaseLongPress: () -> Unit = {},
) {
    val titleContent = @Composable {
        Column(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onCaseLongPress,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderTitle(
                title,
                Modifier.testTag("viewCaseHeaderTitle"),
            )
            HeaderSubTitle(
                subTitle,
                Modifier.testTag("viewCaseHeaderSubTitle"),
            )
        }
    }

    val navigationContent = @Composable { TopBarBackAction(onBack) }

    val actionsContent: (@Composable (RowScope.() -> Unit)) = if (isLoading || hideHeaderActions) {
        @Composable {}
    } else {
        @Composable {
            HeaderActions(
                isEditable = isEditable,
                isHighPriority = isHighPriority,
                isFavorite = isFavorite,
                toggleHighPriority = toggleHighPriority,
                toggleFavorite = toggleFavorite,
            )
        }
    }

    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
        actions = actionsContent,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
private fun ColumnScope.SideHeader(
    title: String,
    subTitle: String = "",
    updatedAtText: String,
    isFavorite: Boolean = false,
    isHighPriority: Boolean = false,
    hideHeaderActions: Boolean = false,
    onBack: () -> Unit = {},
    isLoading: Boolean = false,
    toggleFavorite: () -> Unit = {},
    toggleHighPriority: () -> Unit = {},
    isEditable: Boolean = false,
    onCaseLongPress: () -> Unit = {},
) {
    val titleContent = @Composable {
        HeaderTitle(
            title,
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = onCaseLongPress,
                )
                .testTag("viewCaseHeaderTitle"),
        )
    }

    val navigationContent = @Composable { TopBarBackAction(onBack) }

    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = navigationContent,
        actions = {},
    )

    HeaderSubTitle(
        subTitle,
        listItemModifier.testTag("viewCaseHeaderSubTitle"),
    )

    ViewCaseUpdatedAtView(updatedAtText, listItemModifier)

    if (!(isLoading || hideHeaderActions)) {
        Spacer(Modifier.weight(1f))
        Row(
            listItemModifier,
            horizontalArrangement = listItemSpacedByHalf,
        ) {
            Spacer(Modifier.weight(1f))
            HeaderActions(
                isEditable = isEditable,
                isHighPriority = isHighPriority,
                isFavorite = isFavorite,
                toggleHighPriority = toggleHighPriority,
                toggleFavorite = toggleFavorite,
            )
        }
    }
}
