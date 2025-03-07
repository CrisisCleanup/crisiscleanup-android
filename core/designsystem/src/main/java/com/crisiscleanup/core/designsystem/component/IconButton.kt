package com.crisiscleanup.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.R
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha

@Composable
fun CrisisCleanupElevatedIconButton(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    @DrawableRes iconResId: Int = 0,
    imageVector: ImageVector? = null,
    contentDescriptionResId: Int = 0,
    contentDescription: String = "",
    onClick: () -> Unit = {},
    shape: Shape = RectangleShape,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(
        // TODO Disable elevation from changing container color
        defaultElevation = 8.dp,
    ),
    enabled: Boolean = true,
) {
    ElevatedButton(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        contentPadding = paddingValues,
        elevation = elevation,
        enabled = enabled,
    ) {
        val cd = if (contentDescriptionResId == 0) {
            contentDescription
        } else {
            stringResource(contentDescriptionResId)
        }
        if (iconResId != 0) {
            Icon(
                painterResource(iconResId),
                cd,
                iconModifier,
            )
        } else {
            imageVector?.let {
                Icon(
                    it,
                    cd,
                    iconModifier,
                )
            }
        }
    }
}

@Composable
fun CrisisCleanupIconButton(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    @DrawableRes iconResId: Int = 0,
    imageVector: ImageVector? = null,
    contentDescriptionResId: Int = 0,
    contentDescription: String = "",
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
    ) {
        val cd = if (contentDescriptionResId == 0) {
            contentDescription
        } else {
            stringResource(contentDescriptionResId)
        }
        val iconTint = if (enabled) tint else tint.disabledAlpha()
        if (iconResId != 0) {
            Icon(
                painterResource(iconResId),
                cd,
                iconModifier,
                iconTint,
            )
        } else {
            imageVector?.let {
                Icon(
                    it,
                    cd,
                    iconModifier,
                    iconTint,
                )
            }
        }
    }
}

@Preview(name = "drawable")
@Composable
fun CrisisCleanupDrawableIconButtonPreview() {
    CrisisCleanupElevatedIconButton(
        modifier = Modifier.size(48.dp),
        iconResId = R.drawable.ic_cases,
    )
}

@Preview(name = "vector")
@Composable
fun CrisisCleanupVectorIconButtonPreview() {
    CrisisCleanupElevatedIconButton(
        iconModifier = Modifier.size(96.dp),
        imageVector = CrisisCleanupIcons.Delete,
    )
}
