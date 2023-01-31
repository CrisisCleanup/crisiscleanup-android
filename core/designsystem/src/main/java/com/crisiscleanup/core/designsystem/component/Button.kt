package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private fun roundedRectangleButtonShape() = RoundedCornerShape(4.dp)
private val buttonMinHeight = 48.dp

@Composable
private fun Text(
    @StringRes
    textResId: Int = 0,
    text: String = "",
) {
    if (textResId != 0) {
        Text(stringResource(textResId))
    } else {
        Text(text)
    }
}

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
        modifier = modifier.sizeIn(minHeight = buttonMinHeight),
        onClick = onClick,
        enabled = enabled,
        shape = roundedRectangleButtonShape(),
    ) {
        if (indicateBusy) {
            CircularProgressIndicator()
        } else {
            Text(textResId, text)
        }
    }
}

@Composable
fun CrisisCleanupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    @StringRes
    textResId: Int = 0,
    text: String = "",
) {
    Button(
        modifier = modifier.sizeIn(minHeight = buttonMinHeight),
        onClick = onClick,
        enabled = enabled,
        shape = roundedRectangleButtonShape(),
    ) {
        Text(textResId, text)
    }
}

@Composable
fun CrisisCleanupTextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    @StringRes
    textResId: Int = 0,
    text: String = "",
) {
    TextButton(
        modifier = modifier.sizeIn(minHeight = buttonMinHeight),
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(textResId, text)
    }
}

@Preview
@Composable
fun CrisisCleanupButtonPreview() {
    CrisisCleanupButton(
        enabled = true,
        text = "Press"
    )
}