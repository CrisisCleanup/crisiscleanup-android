package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconTextButton
import com.crisiscleanup.core.designsystem.theme.actionLinkColor
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding

private val errorMessageModifier = Modifier
    .listItemHorizontalPadding()
    .listItemTopPadding()

@Composable
internal fun ErrorText(
    errorMessage: String,
) {
    if (errorMessage.isNotBlank()) {
        Text(
            errorMessage,
            modifier = errorMessageModifier,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
internal fun CrisisCleanupIconTextButton(
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