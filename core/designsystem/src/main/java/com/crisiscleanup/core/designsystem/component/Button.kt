package com.crisiscleanup.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
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

fun Modifier.actionHeight() = this.heightIn(min = 48.dp)
fun Modifier.actionSize() = this.size(48.dp)
fun Modifier.actionSmallSize() = this.size(44.dp)
fun Modifier.actionSmallWidth() = this.size(width = 44.dp, height = 0.dp)
fun Modifier.fabPlusSpaceHeight() = this.size(fabPlusSpaceHeight)

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun Text(
    @StringRes textResId: Int = 0,
    text: String = "",
    style: TextStyle = LocalFontStyles.current.header4,
) {
    Text(
        if (textResId != 0) stringResource(textResId) else text,
        style = style,
    )
}

@Composable
fun BusyButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @StringRes textResId: Int = 0,
    text: String = "",
    indicateBusy: Boolean = false,
    colors: ButtonColors = primaryButtonColors(),
    isSharpCorners: Boolean = false,
    style: TextStyle = LocalFontStyles.current.header4,
    onClick: () -> Unit = {},
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
            CircularProgressIndicator(Modifier.size(LocalDimensions.current.buttonSpinnerSize))
        } else {
            Text(textResId, text, style)
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
    indicateBusy: Boolean = false,
    colors: ButtonColors = primaryButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
) {
    Button(
        modifier = modifier.actionHeight(),
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = roundedRectangleButtonShape(),
        elevation = elevation,
    ) {
        if (indicateBusy) {
            CircularProgressIndicator(Modifier.size(LocalDimensions.current.buttonSpinnerSize))
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
    colors: ButtonColors = primaryButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    content: @Composable () -> Unit = {},
) {
    Button(
        modifier = modifier.actionHeight(),
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = roundedRectangleButtonShape(),
        elevation = elevation,
    ) {
        content()
    }
}

@Composable
fun CrisisCleanupTextButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @StringRes textResId: Int = 0,
    text: String = "",
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    onClick: () -> Unit = {},
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
    indicateBusy: Boolean = false,
    borderColor: Color = LocalContentColor.current,
    fontWeight: FontWeight? = null,
    style: TextStyle = LocalFontStyles.current.header4,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val border = BorderStroke(
        width = 1.dp,
        color = if (enabled) borderColor else borderColor.disabledAlpha(),
    )
    val buttonText = text.ifBlank { if (textResId != 0) stringResource(textResId) else "" }
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = roundedRectangleButtonShape(),
        border = border,
        contentPadding = contentPadding,
    ) {
        if (indicateBusy) {
            CircularProgressIndicator(Modifier.size(LocalDimensions.current.buttonSpinnerSize))
        } else if (buttonText.isNotBlank()) {
            Text(
                buttonText,
                fontWeight = fontWeight,
                style = style,
            )
        }
        trailingContent?.invoke()
    }
}

private val fabElevationZero: FloatingActionButtonElevation
    @Composable
    get() = FloatingActionButtonDefaults.elevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
    )

@OptIn(ExperimentalMaterial3Api::class)
private val disabledRippleConfiguration: RippleConfiguration
    @Composable
    get() = RippleConfiguration(
        color = Color.Transparent,
        rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f),
    )

@OptIn(ExperimentalMaterial3Api::class)
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
    if (enabled) {
        FloatingActionButton(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
            shape = shape,
            onClick = onClick,
        ) {
            iconContent()
        }
    } else {
        CompositionLocalProvider(
            LocalRippleConfiguration provides disabledRippleConfiguration,
        ) {
            FloatingActionButton(
                modifier = modifier,
                containerColor = disabledButtonContainerColor,
                contentColor = disabledButtonContentColor,
                elevation = fabElevationZero,
                shape = shape,
                onClick = {},
            ) {
                iconContent()
            }
        }
    }
}

@Composable
fun WorkTypeAction(
    text: String,
    enabled: Boolean,
    indicateBusy: Boolean = false,
    onClick: () -> Unit = {},
) = CrisisCleanupOutlinedButton(
    // TODO Common dimensions
    modifier = Modifier
        .testTag("workTypeAction_$text")
        .widthIn(100.dp),
    text = text,
    onClick = onClick,
    enabled = enabled,
    indicateBusy = indicateBusy,
)

@Composable
fun WorkTypeBusyAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit = {},
) = WorkTypeAction(text, enabled = enabled, indicateBusy = !enabled, onClick)

@Composable
fun WorkTypePrimaryAction(
    text: String,
    enabled: Boolean,
    indicateBusy: Boolean = false,
    onClick: () -> Unit = {},
) = CrisisCleanupButton(
    // TODO Common dimensions
    modifier = Modifier
        .testTag("workTypePrimaryAction_$text")
        .widthIn(100.dp),
    text = text,
    onClick = onClick,
    enabled = enabled,
    indicateBusy = indicateBusy,
    elevation = ButtonDefaults.buttonElevation(
        defaultElevation = 1.dp,
    ),
)

@Preview
@Composable
fun CrisisCleanupButtonPreview() {
    CrisisCleanupTheme {
        CrisisCleanupButton(
            enabled = true,
            text = "Press",
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
        CrisisCleanupOutlinedButton(text = "Outlined") {
            Icon(
                imageVector = CrisisCleanupIcons.ArrowDropDown,
                contentDescription = null,
            )
        }
    }
}
