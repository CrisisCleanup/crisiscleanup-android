package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.LinkifyHtmlText
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.RegisterSuccessView
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.primaryRedColor
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.InviteDisplayInfo
import com.crisiscleanup.feature.authentication.RequestOrgAccessViewModel
import com.crisiscleanup.feature.authentication.TransferOrgOption
import kotlinx.coroutines.launch
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestOrgAccessRoute(
    onBack: () -> Unit,
    closeRequestAccess: () -> Unit,
    openAuth: () -> Unit = {},
    openForgotPassword: () -> Unit = {},
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val isInviteRequested by viewModel.isInviteRequested.collectAsStateWithLifecycle()
    val isOrgTransferred by viewModel.isOrgTransferred.collectAsStateWithLifecycle()

    val clearStateOnBack = remember(viewModel, isInviteRequested, onBack, closeRequestAccess) {
        {
            viewModel.clearInviteCode()

            if (isInviteRequested) {
                closeRequestAccess()
            } else {
                onBack()
            }
        }
    }

    // TODO Backing out does nothing when directed from paste invite link
    BackHandler {
        clearStateOnBack()
    }

    val screenTitle by viewModel.screenTitle.collectAsStateWithLifecycle()

    val inviteInfoErrorMessage by viewModel.inviteInfoErrorMessage.collectAsStateWithLifecycle()

    Column {
        TopAppBarBackAction(
            title = screenTitle,
            onAction = clearStateOnBack,
        )

        if (viewModel.isRecentlyTransferred) {
            OnOrgTransferredView(
                viewModel.recentOrgTransferredTo,
                openForgotPassword = openForgotPassword,
                openAuth = openAuth,
            )
        } else if (inviteInfoErrorMessage.isNotBlank()) {
            Text(
                inviteInfoErrorMessage,
                listItemModifier.testTag("requestAccessInviteInfoError"),
                style = LocalFontStyles.current.header3,
            )
        } else if (isInviteRequested) {
            val actionText =
                if (viewModel.showEmailInput) "" else LocalAppTranslator.current("actions.login")
            RegisterSuccessView(
                title = viewModel.requestSentTitle,
                text = viewModel.requestSentText,
                actionText = actionText,
                onAction = clearStateOnBack,
            )
        } else if (isOrgTransferred) {
            val displayInfo by viewModel.inviteDisplay.collectAsStateWithLifecycle()
            val orgName = displayInfo?.inviteInfo?.orgName ?: ""
            OnOrgTransferredView(
                orgName,
                openForgotPassword = openForgotPassword,
                openAuth = openAuth,
            )
        } else {
            RequestOrgUserInfoInputView(
                onBack = clearStateOnBack,
            )
        }
    }
}

@Composable
private fun ColumnScope.OnOrgTransferredView(
    orgName: String,
    openForgotPassword: () -> Unit,
    openAuth: () -> Unit,
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val clearStateOpenAuth = remember(viewModel, openAuth) {
        {
            viewModel.clearInviteCode()
            openAuth()
        }
    }
    val clearStateForgotPassword = remember(viewModel, openForgotPassword) {
        {
            viewModel.clearInviteCode()
            openForgotPassword()
        }
    }

    OrgTransferSuccessView(
        orgName,
        onForgotPassword = clearStateForgotPassword,
        onLogin = clearStateOpenAuth,
    )
}

@Composable
private fun RequestOrgUserInfoInputView(
    onBack: () -> Unit,
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val displayInfo by viewModel.inviteDisplay.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    if (viewModel.isFromInvite && displayInfo == null) {
        Box(Modifier.fillMaxSize()) {
            BusyIndicatorFloatingTopCenter(true)
        }
    } else {
        val closeKeyboard = rememberCloseKeyboard()

        var contentSize by remember { mutableStateOf(Size.Zero) }

        val scrollState = rememberScrollState()
        Column(
            Modifier
                .scrollFlingListener(closeKeyboard)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .onGloballyPositioned {
                    contentSize = it.size.toSize()
                },
        ) {
            val isExistingUser = displayInfo?.inviteInfo?.isExistingUser == true
            val isEditable by viewModel.isEditable.collectAsStateWithLifecycle()

            if (isExistingUser) {
                InviteExistingUserContent(
                    onBack = onBack,
                    isEditable = isEditable,
                    isLoading = isLoading,
                    displayInfo!!,
                )
            } else {
                InviteNewUserContent(
                    contentSize,
                    scrollState,
                    isEditable = isEditable,
                    isLoading = isLoading,
                    displayInfo,
                )
            }
        }
    }
}

@Composable
internal fun InviterAvatar(
    avatarUrl: URL,
    displayName: String,
    inviteMessage: String,
) {
    Row(
        listItemModifier,
        horizontalArrangement = listItemSpacedBy,
    ) {
        val actionIcon = CrisisCleanupIcons.QuestionMark
        val fallbackPainter = rememberVectorPainter(actionIcon)
        val placeholderPainter = rememberVectorPainter(CrisisCleanupIcons.Account)
        // TODO Show error as necessary
        AsyncImage(
            modifier = Modifier
                // TODO Common dimensions
                .size(48.dp)
                .clip(CircleShape),
            model = avatarUrl.toString(),
            contentDescription = displayName,
            fallback = fallbackPainter,
            contentScale = ContentScale.FillBounds,
            placeholder = placeholderPainter,
        )

        Column {
            Text(
                displayName,
                Modifier.testTag("inviterAvatarDisplayName"),
                style = LocalFontStyles.current.header4,
            )
            Text(
                inviteMessage,
                Modifier.testTag("inviterAvatarMessage"),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun InviteExistingUserContent(
    onBack: () -> Unit,
    isEditable: Boolean,
    isLoading: Boolean,
    displayInfo: InviteDisplayInfo,
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current
    val translationCount by t.translationCount.collectAsStateWithLifecycle()

    var selectedOrgTransfer by remember { mutableStateOf(TransferOrgOption.NotSelected) }

    val inviteInfo = displayInfo.inviteInfo
    val transferInstructions = t("invitationSignup.inviting_to_transfer_confirm")
        .replace("{user}", inviteInfo.displayName)
        .replace("{fromOrg}", inviteInfo.fromOrgName)
        .replace("{toOrg}", inviteInfo.orgName)

    LinkifyHtmlText(
        transferInstructions,
        listItemModifier,
    )

    for (option in viewModel.transferOrgOptions) {
        CrisisCleanupRadioButton(
            listItemModifier,
            option == selectedOrgTransfer,
            text = t(option.translateKey),
            onSelect = {
                selectedOrgTransfer = option
                viewModel.onChangeTransferOrgOption()
            },
            enabled = isEditable,
        )
    }

    val errorMessage = viewModel.transferOrgErrorMessage
    if (errorMessage.isNotBlank()) {
        Text(
            errorMessage,
            listItemModifier,
            color = primaryRedColor,
        )
    }

    val transferText = remember(translationCount) {
        t("actions.transfer")
    }
    BusyButton(
        fillWidthPadded.testTag("transferOrgSubmitAction"),
        enabled = isEditable && selectedOrgTransfer != TransferOrgOption.NotSelected,
        text = transferText,
        indicateBusy = isLoading,
        onClick = {
            if (selectedOrgTransfer == TransferOrgOption.DoNotTransfer) {
                onBack()
            } else {
                viewModel.onTransferOrg(selectedOrgTransfer)
            }
        },
    )
}

@Composable
private fun ColumnScope.OrgTransferSuccessView(
    orgName: String,
    onForgotPassword: () -> Unit,
    onLogin: () -> Unit,
) {
    val t = LocalAppTranslator.current

    Text(
        t("invitationSignup.move_completed"),
        listItemModifier,
        style = MaterialTheme.typography.headlineSmall,
    )

    Text(
        t("invitationSignup.congrats_move_complete")
            .replace("{toOrg}", orgName),
        listItemModifier,
    )

    Spacer(Modifier.weight(1f))

    CrisisCleanupOutlinedButton(
        modifier = listItemModifier
            .actionHeight(),
        enabled = true,
        onClick = onForgotPassword,
        text = t("invitationSignup.forgot_password"),
    )

    BusyButton(
        modifier = listItemModifier
            .actionHeight(),
        onClick = onLogin,
        text = t("actions.login"),
    )
}

@Composable
private fun InviteNewUserContent(
    contentSize: Size,
    scrollState: ScrollState,
    isEditable: Boolean,
    isLoading: Boolean,
    displayInfo: InviteDisplayInfo?,
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current
    val translationCount by t.translationCount.collectAsStateWithLifecycle()

    val clearErrorVisuals = viewModel::clearErrors

    if (viewModel.showEmailInput) {
        val requestInstructions = t("requestAccess.request_access_enter_email")
        Text(
            requestInstructions,
            listItemModifier.testTag("requestAccessByEmailInstructions"),
        )

        val hasEmailError = viewModel.emailAddressError.isNotBlank()
        if (hasEmailError) {
            Text(
                viewModel.emailAddressError,
                Modifier
                    .listItemHorizontalPadding()
                    .listItemTopPadding()
                    .testTag("requestAccessByEmailError"),
                color = MaterialTheme.colorScheme.error,
            )
        }
        // TODO Initial focus isn't taking (on emulator)
        val hasEmailFocus = viewModel.emailAddress.isBlank() || hasEmailError
        OutlinedClearableTextField(
            modifier = listItemModifier.testTag("requestAccessByEmailTextField"),
            label = t("requestAccess.existing_member_email"),
            value = viewModel.emailAddress,
            onValueChange = { viewModel.emailAddress = it },
            keyboardType = KeyboardType.Email,
            enabled = isEditable,
            isError = hasEmailError,
            hasFocus = hasEmailFocus,
            onNext = clearErrorVisuals,
        )
    } else {
        if (displayInfo == null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                AnimatedBusyIndicator(
                    isLoading,
                    padding = 16.dp,
                )
            }
        } else {
            val info = displayInfo
            val avatarUrl = displayInfo.avatarUrl
            if (avatarUrl != null &&
                info.displayName.isNotBlank() &&
                info.inviteMessage.isNotBlank()
            ) {
                InviterAvatar(
                    avatarUrl,
                    displayName = info.displayName,
                    inviteMessage = info.inviteMessage,
                )
            }
        }
    }

    Text(
        t("requestAccess.complete_form_request_access"),
        fillWidthPadded.testTag("requestAccessInputInstruction"),
        style = LocalFontStyles.current.header3,
    )

    val coroutineScope = rememberCoroutineScope()
    val languageOptions by viewModel.languageOptions.collectAsStateWithLifecycle()
    UserInfoInputView(
        infoData = viewModel.userInfo,
        languageOptions = languageOptions,
        isEditable = isEditable,
        onEndOfInput = {
            coroutineScope.launch {
                scrollState.animateScrollTo(contentSize.height.toInt())
            }
        },
    )

    Text(
        t("requestAccess.request_will_be_sent"),
        listItemModifier.testTag("requestAccessSubmitExplainer"),
    )

    val requestAccessText = remember(translationCount) {
        t("actions.request_access")
    }
    BusyButton(
        fillWidthPadded.testTag("requestOrgAccessSubmitAction"),
        enabled = isEditable,
        text = requestAccessText,
        indicateBusy = isLoading,
        onClick = viewModel::onVolunteerWithOrg,
    )
}
