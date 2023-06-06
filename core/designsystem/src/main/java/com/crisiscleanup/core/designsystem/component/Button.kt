package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.cancelButtonContainerColor
import com.crisiscleanup.core.designsystem.theme.cancelButtonContentColor
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.disabledButtonContainerColor
import com.crisiscleanup.core.designsystem.theme.disabledButtonContentColor

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
val actionEdgeSpace = 16.dp

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

private val fabPlusSpaceHeight = 48.dp.plus(actionEdgeSpace.times(2))

fun Modifier.actionHeight() = heightIn(min = 48.dp)
fun Modifier.actionSize() = size(48.dp)
fun Modifier.actionSmallSize() = size(44.dp)
fun Modifier.fabPlusSpaceHeight() = size(fabPlusSpaceHeight)

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
    isSharpCorners: Boolean = false,
) {
    val shape = if (isSharpCorners) RectangleShape else roundedRectangleButtonShape()
    Button(
        modifier = modifier.actionHeight(),
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = if (indicateBusy) null else ButtonDefaults.elevatedButtonElevation(),
    ) {
        if (indicateBusy) {
            // TODO Common dimensions
            CircularProgressIndicator(Modifier.size(24.dp))
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
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
) {
    Button(
        modifier = modifier.actionHeight(),
        onClick = onClick,
        enabled = enabled,
        colors = primaryButtonColors(),
        shape = roundedRectangleButtonShape(),
        elevation = elevation,
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
    elevation: ButtonElevation? = null,
) = TextButton(
    modifier = modifier.actionHeight(),
    onClick = onClick,
    enabled = enabled,
    colors = colors,
    shape = roundedRectangleButtonShape(),
    elevation = elevation,
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

private val NoRippleTheme = object : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Transparent

    @Composable
    override fun rippleAlpha() = RippleAlpha(0f, 0f, 0f, 0f)
}

@Composable
fun CrisisCleanupFab(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    iconContent: @Composable () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalRippleTheme provides if (enabled) LocalRippleTheme.current else NoRippleTheme
    ) {
        FloatingActionButton(
            modifier = modifier,
            containerColor = if (enabled) containerColor else disabledButtonContainerColor,
            contentColor = if (enabled) contentColor else disabledButtonContentColor,
            elevation = if (enabled) elevation else FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
            ),
            shape = shape,
            onClick = {
                if (enabled) {
                    onClick()
                }
            },
        ) {
            iconContent()
        }
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