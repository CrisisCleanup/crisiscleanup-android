package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private fun roundedRectangleButtonShape() = RoundedCornerShape(4.dp)

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
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    @StringRes
    textResId: Int = 0,
    text: String = "",
    indicateBusy: Boolean = false,
) {
    Button(
        modifier = modifier.actionHeight(),
        onClick = onClick,
        enabled = enabled,
        shape = roundedRectangleButtonShape(),
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
    @StringRes
    textResId: Int = 0,
    text: String = "",
) {
    Button(
        modifier = modifier.actionHeight(),
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
        modifier = modifier.actionHeight(),
        onClick = onClick,
        enabled = enabled,
        shape = roundedRectangleButtonShape(),
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

@Preview
@Composable
fun BusyButtonPreview() {
    // Disable button to see progress or colors will be matching
    BusyButton(
        enabled = false,
        text = "Press",
        indicateBusy = true,
    )
}