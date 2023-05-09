package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.cancelButtonContainerColor
import com.crisiscleanup.core.designsystem.theme.cancelButtonContentColor
import com.crisiscleanup.core.designsystem.theme.disabledAlpha

private fun roundedRectangleButtonShape() = RoundedCornerShape(4.dp)

@Composable
fun cancelButtonColors() = ButtonDefaults.buttonColors(
    containerColor = cancelButtonContainerColor,
    contentColor = cancelButtonContentColor,
)

val mapButtonSize = 48.dp
val mapButtonEdgeSpace = 8.dp
val adjacentButtonSpace = 1.dp

val actionInnerSpace = 16.dp
val actionEdgeSpace = 20.dp

val actionRoundCornerRadius = 4.dp
val actionRoundCornerShape = RoundedCornerShape(actionRoundCornerRadius)
val actionTopRoundCornerShape = RoundedCornerShape(
    topStart = actionRoundCornerRadius,
    topEnd = actionRoundCornerRadius,
)
val actionBottomRoundCornerShape = RoundedCornerShape(
    bottomStart = actionRoundCornerRadius,
    bottomEnd = actionRoundCornerRadius,
)
val actionStartRoundCornerShape = RoundedCornerShape(
    topStart = actionRoundCornerRadius,
    bottomStart = actionRoundCornerRadius,
)
val actionEndRoundCornerShape = RoundedCornerShape(
    topEnd = actionRoundCornerRadius,
    bottomEnd = actionRoundCornerRadius,
)
val actionSmallSpace = 12.dp

fun Modifier.actionHeight() = heightIn(48.dp)
fun Modifier.actionSize() = size(48.dp)
fun Modifier.actionSmallSize() = size(44.dp)

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun Text(
    @StringRes textResId: Int = 0,
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
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    @StringRes textResId: Int = 0,
    text: String = "",
    indicateBusy: Boolean = false,
    colors: ButtonColors = primaryButtonColors(),
) {
    Button(
        modifier = modifier.actionHeight(),
        onClick = onClick,
        enabled = enabled,
        shape = roundedRectangleButtonShape(),
        colors = colors,
        elevation = if (indicateBusy) null else ButtonDefaults.elevatedButtonElevation(),
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
    @StringRes textResId: Int = 0,
    text: String = "",
) {
    Button(
        modifier = modifier.actionHeight(),
        onClick = onClick,
        enabled = enabled,
        colors = primaryButtonColors(),
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
    @StringRes textResId: Int = 0,
    text: String = "",
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
) = TextButton(
    modifier = modifier.actionHeight(),
    onClick = onClick,
    enabled = enabled,
    colors = colors,
    shape = roundedRectangleButtonShape(),
) {
    Text(textResId, text)
}

@Composable
fun CrisisCleanupOutlinedButton(
    modifier: Modifier = Modifier,
    @StringRes textResId: Int = 0,
    text: String = "",
    onClick: () -> Unit = {},
    enabled: Boolean = false,
    borderColor: Color = LocalContentColor.current,
) {
    val border = BorderStroke(
        width = 1.dp,
        color = if (enabled) borderColor else borderColor.disabledAlpha(),
    )
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = roundedRectangleButtonShape(),
        border = border,
    ) {
        Text(text.ifEmpty { if (textResId != 0) stringResource(textResId) else "" })
    }
}

@Preview
@Composable
fun CrisisCleanupButtonPreview() {
    CrisisCleanupTheme {
        CrisisCleanupButton(
            enabled = true,
            text = "Press"
        )
    }
}

@Preview
@Composable
fun BusyButtonPreview() {
    CrisisCleanupTheme {
        // Disable button to see progress or colors will be matching
        BusyButton(
            enabled = false,
            text = "Press",
            indicateBusy = true,
        )
    }
}

@Preview
@Composable
fun CrisisCleanupOutlinedButtonPreview() {
    CrisisCleanupTheme {
        CrisisCleanupOutlinedButton(text = "Outlined")
    }
}