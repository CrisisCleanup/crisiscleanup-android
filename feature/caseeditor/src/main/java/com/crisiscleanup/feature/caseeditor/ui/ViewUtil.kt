package com.crisiscleanup.feature.caseeditor.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
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
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

@Composable
fun rememberCloseKeyboard(rememberKey: Any): () -> Unit {
    val isKeyboardOpen by keyboardAsState()
    val focusManager = LocalFocusManager.current
    return remember(rememberKey) {
        {
            Log.w("scroll-close-keyboard", "Close keyboard $isKeyboardOpen")
            if (isKeyboardOpen) {
                focusManager.clearFocus(true)
            }
        }
    }
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