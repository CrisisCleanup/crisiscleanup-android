package com.crisiscleanup.feature.authentication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.DayNightPreviews
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.actionLinkColor
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.feature.authentication.R

@Composable
internal fun ConditionalErrorMessage(errorMessage: String) {
    if (errorMessage.isNotEmpty()) {
        Text(
            modifier = fillWidthPadded,
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
    action: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
    ) {
        Text(
            text = translator(textTranslateKey),
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    onClick = action,
                )
                .then(modifier),
            style = LocalFontStyles.current.header4,
            color = primaryBlueColor,
        )
    }
}

@Composable
internal fun CrisisCleanupLogoRow() {
    // TODO Adjust to other screen sizes as necessary
    Box(Modifier.padding(top = 16.dp, start = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Image(
                painterResource(R.drawable.worker_wheelbarrow_world_background),
                modifier = Modifier
                    .testTag("ccuBackground")
                    .padding(top = 32.dp)
                    .size(width = 480.dp, height = 240.dp)
                    .offset(x = 64.dp),
                contentScale = ContentScale.FillHeight,
                contentDescription = null,
            )
        }
        Row(
            modifier = fillWidthPadded,
            horizontalArrangement = Arrangement.Start,
        ) {
            Image(
                modifier = Modifier
                    .testTag("ccuLogo")
                    .sizeIn(maxWidth = 160.dp),
                painter = painterResource(com.crisiscleanup.core.common.R.drawable.crisis_cleanup_logo),
                contentDescription = stringResource(com.crisiscleanup.core.common.R.string.crisis_cleanup),
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
        modifier = modifier.padding(horizontal = 16.dp),
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        enabled = enabled,
    ) {
        val text = translator(
            "~~Login using different method",
            R.string.loginUsingDifferentMethod,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.ArrowBack,
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
