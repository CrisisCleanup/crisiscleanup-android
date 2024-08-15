package com.crisiscleanup.feature.authentication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupLogoRow
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.DayNightPreviews
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.actionLinkColor
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.feature.authentication.R

@Composable
internal fun ConditionalErrorMessage(
    errorMessage: String,
    testTagPrefix: String,
) {
    if (errorMessage.isNotEmpty()) {
        Text(
            modifier = fillWidthPadded.testTag("${testTagPrefix}Error"),
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
internal fun LinkAction(
    textTranslateKey: String,
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.End,
    enabled: Boolean = false,
    color: Color = primaryBlueColor,
    action: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    onClick = action,
                )
                .then(modifier),
        ) {
            Text(
                text = translator(textTranslateKey),
                modifier = Modifier.align(Alignment.CenterEnd),
                style = LocalFontStyles.current.header4,
                color = if (enabled) color else color.disabledAlpha(),
            )
        }
    }
}

@Composable
fun LoginWithDifferentMethod(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = false,
) {
    val translator = LocalAppTranslator.current
    TextButton(
        modifier = modifier
            .testTag("loginWithDifferentMethodAction")
            .padding(horizontal = 16.dp),
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        enabled = enabled,
    ) {
        val text = translator(
            "loginForm.use_different_method",
            R.string.loginUsingDifferentMethod,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                CrisisCleanupIcons.ArrowBack2,
                contentDescription = text,
                tint = actionLinkColor,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(24.dp),
            )
            Text(
                text = text,
                color = actionLinkColor,
                style = LocalFontStyles.current.header3,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@DayNightPreviews
@Composable
fun LogoRowPreview() {
    CrisisCleanupTheme {
        CrisisCleanupLogoRow()
    }
}

@DayNightPreviews
@Composable
fun LoginWithDifferentMethodPreview() {
    CrisisCleanupTheme {
        LoginWithDifferentMethod(
            onClick = {},
        )
    }
}
