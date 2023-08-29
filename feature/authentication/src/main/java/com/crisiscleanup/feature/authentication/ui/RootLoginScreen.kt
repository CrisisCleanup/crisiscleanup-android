package com.crisiscleanup.feature.authentication.ui

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.LinkifyText
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.DayNightPreviews
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.feature.authentication.R


@Composable
internal fun RootLoginScreen() {
    val translator = LocalAppTranslator.current
    val uriHandler = LocalUriHandler.current
    val isBusy = false
    Text(
        modifier = listItemModifier.testTag("loginHeaderText"),
        text = translator("actions.login", R.string.login),
        style = LocalFontStyles.current.header1,
    )
    Column(
        modifier = fillWidthPadded,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BusyButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginLoginWithEmailBtn"),
            onClick = {},
            enabled = !isBusy,
            text = translator("~~Login with Email", R.string.loginWithEmail),
            indicateBusy = isBusy,
        )
        BusyButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginLoginWithPhoneBtn"),
            onClick = {},
            enabled = !isBusy,
            text = translator("~~Login with Cell Phone", R.string.loginWithPhone),
            indicateBusy = isBusy,
        )
        CrisisCleanupOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginVolunteerWithOrgBtn"),
            onClick = {},
            enabled = !isBusy,
            text = translator("~~Volunteer with Your Org", R.string.volunteerWithYourOrg),
        )
        // TODO Open in WebView?
        CrisisCleanupOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginNeedHelpCleaningBtn"),
            onClick = {
                uriHandler.openUri("https://crisiscleanup.org/survivor")
            },
            enabled = !isBusy,
            text = translator("~~I need help cleaning up", R.string.iNeedHelpCleaningUp),
        )
    }
    Column(
        modifier = fillWidthPadded,
    ) {
        val linkText = translator("~~Register here", R.string.registerHere)
        val spannableString = SpannableString(linkText).apply {
            setSpan(
                URLSpan("https://crisiscleanup.org/register"),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        Text(
            modifier = Modifier.testTag("loginReliefOrgAndGovText"),
            text = translator(
                "~~Relief organizations and government only.",
                R.string.reliefOrgAndGovOnly,
            ),
        )
        LinkifyText(
            modifier = Modifier.testTag("loginRegisterHereLink"),
            text = spannableString,
            linkify = { textView ->
                textView.movementMethod = LinkMovementMethod.getInstance()
            },
        )
    }
}

@DayNightPreviews
@Composable
fun RootLoginScreenPreview() {
    CrisisCleanupTheme {
        RootLoginScreen()
    }
}