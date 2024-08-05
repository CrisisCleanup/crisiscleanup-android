package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.DayNightPreviews
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.statusClosedColor

@Composable
fun RegisterSuccessView(
    title: String,
    text: String,
    actionText: String = "",
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            modifier = Modifier.size(64.dp),
            imageVector = CrisisCleanupIcons.CheckCircle,
            contentDescription = null,
            tint = statusClosedColor,
        )

        Text(
            title,
            listItemModifier.testTag("registerSuccessTitle"),
            style = LocalFontStyles.current.header1,
            textAlign = TextAlign.Center,
        )

        Text(
            text,
            listItemModifier.testTag("registerSuccessText"),
            textAlign = TextAlign.Center,
        )

        if (actionText.isNotBlank()) {
            CrisisCleanupButton(
                modifier = fillWidthPadded,
                text = actionText,
                onClick = onAction,
            )
        }

        Spacer(Modifier.weight(1f))

        CrisisCleanupLogoRow(Modifier, true)

        Spacer(Modifier.weight(1f))
    }
}

@Preview
@DayNightPreviews
@Composable
private fun RegisterSuccessViewPreview() {
    CrisisCleanupTheme {
        RegisterSuccessView(
            title = "Success title",
            text = "Very long overflowing message spilling over the extremities",
            actionText = "Login",
        )
    }
}
