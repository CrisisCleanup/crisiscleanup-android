package com.crisiscleanup.feature.authentication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.feature.authentication.PasteOrgInviteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerPasteInviteLinkRoute(
    onBack: () -> Unit,
    openOrgInvite: (String) -> Unit = {},
    viewModel: PasteOrgInviteViewModel = hiltViewModel(),
) {
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val isVerified = inviteCode.isNotBlank()
    if (isVerified) {
        openOrgInvite(inviteCode)
    }

    val t = LocalAppTranslator.current

    val error by viewModel.inviteCodeError.collectAsStateWithLifecycle()

    var inviteLink by remember { mutableStateOf("") }

    val isVerifying by viewModel.isVerifyingCode.collectAsStateWithLifecycle()

    Column {
        TopAppBarBackAction(
            title = t("nav.invitation_link"),
            onAction = onBack,
        )

        Text(
            t("pasteInvite.paste_invitation_link_and_accept"),
            listItemModifier.testTag("pasteOrgInviteText"),
        )

        if (error.isNotBlank()) {
            Text(
                error,
                listItemModifier.testTag("pasteOrgInviteError"),
                color = MaterialTheme.colorScheme.error,
            )
        }

        OutlinedClearableTextField(
            modifier = listItemModifier.testTag("pasteOrgInviteTextField"),
            label = t("pasteInvite.invite_link"),
            value = inviteLink,
            onValueChange = { inviteLink = it },
            keyboardType = KeyboardType.Uri,
            enabled = !isVerifying,
            isError = error.isNotBlank(),
            hasFocus = inviteLink.isBlank(),
            imeAction = ImeAction.Done,
            onEnter = {
                viewModel.onSubmitLink(inviteLink)
            },
        )

        BusyButton(
            modifier = fillWidthPadded.testTag("pasteOrgInviteSubmitAction"),
            text = t("actions.accept_invite"),
            indicateBusy = isVerifying || isVerified,
        ) {
            viewModel.onSubmitLink(inviteLink)
        }
    }
}
