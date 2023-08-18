package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle

@Composable
fun CrisisCleanupTextCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    @StringRes textResId: Int = 0,
    text: String = "",
    onToggle: () -> Unit = {},
    onCheckChange: (Boolean) -> Unit = {},
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = true,
    enableToggle: Boolean = true,
    spaceTrailingContent: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .clickable(
                enabled = enabled && enableToggle,
                onClick = onToggle,
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckChange,
            enabled = enabled,
        )
        val textValue = if (textResId == 0) text else stringResource(textResId)
        if (textValue.isNotBlank()) {
            Text(
                textValue,
                style = textStyle,
            )
        }
        trailingContent?.let {
            if (spaceTrailingContent) {
                Spacer(Modifier.weight(1f))
            }

            it()
        }
    }
}
