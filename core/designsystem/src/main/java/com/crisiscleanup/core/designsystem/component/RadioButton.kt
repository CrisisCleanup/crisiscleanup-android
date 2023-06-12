package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle

@Composable
fun CrisisCleanupRadioButton(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    @StringRes textResId: Int = 0,
    text: String = "",
    onSelect: () -> Unit = {},
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = true,
    enableToggle: Boolean = true,
    spaceTrailingContent: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .selectable(
                selected = selected,
                enabled = enabled && enableToggle,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
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