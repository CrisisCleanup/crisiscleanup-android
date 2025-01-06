package com.crisiscleanup.feature.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.LinkifyText
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.neutralFontColor
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerOrgRoute(
    onBack: () -> Unit,
    openPasteOrgInviteLink: () -> Unit = {},
    openRequestOrgAccess: () -> Unit = {},
    openScanOrgQrCode: () -> Unit = {},
) {
    val t = LocalAppTranslator.current
    val closeKeyboard = rememberCloseKeyboard()

    Column {
        TopAppBarBackAction(
            title = t("actions.sign_up"),
            onAction = onBack,
        )
        Column(
            Modifier
                .scrollFlingListener(closeKeyboard)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val getStartedInstruction = t("volunteerOrg.get_started_join_org")
            Text(
                getStartedInstruction,
                listItemModifier
                    .testTag("volunteerGetStartedText"),
                style = LocalFontStyles.current.header2,
            )

            InstructionTextAction(
                instruction = t("volunteerOrg.click_inviation_link"),
                actionText = t("volunteerOrg.paste_invitation_link"),
                onAction = openPasteOrgInviteLink,
                modifier = Modifier.testTag("volunteerPasteLinkAction"),
            )

            StaticOrTextView()

            InstructionTextAction(
                instruction = t("volunteerOrg.if_you_know_email"),
                actionText = t("volunteerOrg.request_access"),
                onAction = openRequestOrgAccess,
                modifier = Modifier.testTag("volunteerRequestAccessAction"),
            )

            StaticOrTextView()

            InstructionAction(
                instruction = t("volunteerOrg.find_qr_code"),
                content = {
                    CrisisCleanupButton(
                        modifier = Modifier
                            .testTag("volunteerScanQrCodeAction")
                            .fillMaxWidth()
                            .listItemVerticalPadding(),
                        onClick = openScanOrgQrCode,
                    ) {
                        val qrCodeLabel = t("volunteerOrg.scan_qr_code")
                        Icon(
                            imageVector = CrisisCleanupIcons.QrCode,
                            contentDescription = qrCodeLabel,
                        )
                        Text(
                            qrCodeLabel,
                            // TODO Common dimensions
                            Modifier.padding(start = 8.dp),
                        )
                    }
                },
            )

            Column(fillWidthPadded) {
                Text(t("volunteerOrg.if_no_account"))
                LinkifyText(
                    modifier = Modifier.testTag("volunteerRegisterOrgAction"),
                    linkText = t("registerOrg.register_org"),
                    link = ORG_REGISTER_URL,
                )
            }
        }
    }
}

@Composable
private fun StaticOrTextView() {
    val orText = LocalAppTranslator.current("volunteerOrg.or")
    Text(
        orText,
        fillWidthPadded,
        style = LocalFontStyles.current.header4,
        textAlign = TextAlign.Center,
        color = neutralFontColor,
    )
}

@Composable
private fun InstructionAction(
    instruction: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .background(Color.White)
            .then(fillWidthPadded),
    ) {
        Text(instruction)
        content()
    }
}

@Composable
private fun InstructionTextAction(
    instruction: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InstructionAction(instruction) {
        BusyButton(
            modifier = Modifier
                .fillMaxWidth()
                .listItemTopPadding()
                .then(modifier),
            text = actionText,
            onClick = onAction,
        )
    }
}
