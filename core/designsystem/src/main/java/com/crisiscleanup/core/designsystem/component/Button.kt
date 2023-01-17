package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun BusyButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    @StringRes
    textResId: Int = 0,
    text: String = "",
    indicateBusy: Boolean = false,
) {
    Button(
        modifier = modifier.sizeIn(minHeight = 48.dp),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
    ) {
        if (indicateBusy) {
            CircularProgressIndicator()
        } else {
            if (textResId != 0) {
                Text(stringResource(textResId))
            } else if (text.isNotEmpty()) {
                Text(text)
            }
        }
    }
}