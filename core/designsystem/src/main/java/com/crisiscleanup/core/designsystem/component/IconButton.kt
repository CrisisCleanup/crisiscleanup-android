package com.crisiscleanup.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.R

@Composable
fun CrisisCleanupIconButton(
    modifier: Modifier = Modifier,
    @DrawableRes
    iconRes: Int,
    contentDescriptionResId: Int = 0,
    contentDescription: String = "",
    onClick: () -> Unit = {},
    shape: Shape = RectangleShape,
    paddingValues: PaddingValues = PaddingValues(0.dp),
) {
    ElevatedButton(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        contentPadding = paddingValues,
    ) {
        val cd = if (contentDescriptionResId == 0) contentDescription
        else stringResource(contentDescriptionResId)
        Icon(
            painter = painterResource(iconRes),
            contentDescription = cd,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(name = "square")
@Composable
fun CrisisCleanupIconButtonPreview() {
    CrisisCleanupIconButton(
        modifier = Modifier.size(48.dp),
        iconRes = R.drawable.ic_cases,
    )
}