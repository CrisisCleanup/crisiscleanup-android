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