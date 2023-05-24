package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle

@Composable
fun CrisisCleanupTextRadioButton(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    @StringRes textResId: Int = 0,
    text: String = "",
    onSelect: () -> Unit = {},
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
                onClick = onSelect,
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled,
        )
        val textValue = if (textResId == 0) text else stringResource(textResId)
        if (textValue.isNotBlank()) {
            androidx.compose.material3.Text(
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