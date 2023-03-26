package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.feature.caseeditor.R

private val errorMessageModifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)

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

internal val columnItemModifier = Modifier
    .fillMaxWidth()
    .padding(16.dp, 8.dp)

@Composable
fun rememberCloseKeyboard(rememberKey: Any): () -> Unit {
    val focusManager = LocalFocusManager.current
    return remember(rememberKey) { { focusManager.clearFocus(true) } }
}

@Composable
fun EditCaseSummaryHeader(
    @StringRes headerResId: Int,
    isEditable: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    header: String = "",
    horizontalPadding: Dp = 24.dp,
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
                // TODO Consistent styling
                .padding(16.dp),
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

        // TODO Consistent styling
        Column(
            modifier.padding(
                horizontal = horizontalPadding,
                vertical = 8.dp,
            )
        ) {
            content()
        }
    }
}