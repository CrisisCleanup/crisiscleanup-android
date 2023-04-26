package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconTextButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.feature.caseeditor.R

private val errorMessageModifier = Modifier
    .listItemHorizontalPadding()
    .listItemTopPadding()

@Composable
internal fun ErrorText(
    errorMessage: String,
) {
    if (errorMessage.isNotEmpty()) {
        Text(
            errorMessage,
            modifier = errorMessageModifier,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
internal fun IconButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    @DrawableRes iconResId: Int = 0,
    label: String = "",
    onClick: () -> Unit = {},
    enabled: Boolean = false,
) {
    CrisisCleanupIconTextButton(
        modifier = modifier,
        imageVector = imageVector,
        iconResId = iconResId,
        label = label,
        onClick = onClick,
        enabled = enabled,
        iconTint = actionLinkColor,
        textColor = actionLinkColor,
    )
}

@Composable
fun EditCaseSummaryHeader(
    @StringRes headerResId: Int,
    isEditable: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    header: String = "",
    noContentPadding: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit) = {},
) {
    Column(modifier.fillMaxWidth()) {
        val style = MaterialTheme.typography.headlineSmall
        Row(
            modifier
                .clickable(
                    enabled = isEditable,
                    onClick = onEdit
                )
                .listItemHeight()
                .listItemPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerText = if (headerResId == 0) header
            else stringResource(headerResId)
            Text(
                text = headerText,
                Modifier.weight(1f),
                style = style,
            )
            if (isEditable) {
                Icon(
                    imageVector = CrisisCleanupIcons.Edit,
                    contentDescription = stringResource(R.string.edit_section, headerText),
                    // TODO Use color consistent with system
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        val contentModifier =
            if (noContentPadding) modifier else modifier
                .listItemPadding()
                .listItemNestedPadding()
        Column(contentModifier) {
            content()
        }
    }
}