package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.RegisterSuccessView
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.InviteDisplayInfo
import com.crisiscleanup.feature.authentication.OrgPersistentInviteViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgPersistentInviteRoute(
    onBack: () -> Unit,
    closeInvite: () -> Unit,
    viewModel: OrgPersistentInviteViewModel = hiltViewModel(),
) {
    val isInviteAccepted by viewModel.isInviteAccepted.collectAsStateWithLifecycle()
    val onClose = remember(onBack, closeInvite, isInviteAccepted, viewModel) {
        {
            if (isInviteAccepted) {
                closeInvite()
            } else {
                onBack()
            }
        }
    }

    BackHandler {
        onClose()
    }

    val t = LocalAppTranslator.current

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inviteFailMessage by viewModel.inviteFailMessage.collectAsStateWithLifecycle()
    val inviteDisplay by viewModel.inviteDisplay.collectAsStateWithLifecycle()

    Box {
        Column {
            TopAppBarBackAction(
                title = t("actions.sign_up"),
                onAction = onClose,
            )

            if (inviteFailMessage.isNotBlank()) {
                Text(
                    inviteFailMessage,
                    listItemModifier,
                    style = LocalFontStyles.current.header3,
                )
            } else if (isInviteAccepted) {
                RegisterSuccessView(title = viewModel.acceptedTitle, text = "")
            } else if (inviteDisplay != null) {
                val inviteInfo = inviteDisplay!!.inviteInfo
                if (inviteInfo.isExpiredInvite) {
                    Text(
                        t("persistentInvitations.expired_or_invalid"),
                        listItemModifier,
                        style = LocalFontStyles.current.header3,
                    )
                } else {
                    PersistentInviteInfoInputView(
                        inviteDisplay!!,
                    )
                }
            }
        }

        BusyIndicatorFloatingTopCenter(isLoading)
    }
}

@Composable
private fun PersistentInviteInfoInputView(
    inviteDisplay: InviteDisplayInfo,
    viewModel: OrgPersistentInviteViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current
    val closeKeyboard = rememberCloseKeyboard(viewModel)

    val isEditable by viewModel.isEditable.collectAsStateWithLifecycle()
    val isJoiningOrg by viewModel.isJoiningOrg.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    var contentSize by remember { mutableStateOf(Size.Zero) }
    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .fillMaxSize()
            .verticalScroll(scrollState)
            .onGloballyPositioned {
                contentSize = it.size.toSize()
            },
    ) {
        if (inviteDisplay.inviteInfo.expiration.minus(Clock.System.now()) < 1.days) {
            val expirationText = t("persistentInvitations.invite_expires_in_x_days")
                .replace("{relative_time}", inviteDisplay.inviteInfo.expiration.relativeTime)
            Text(
                expirationText,
                listItemModifier,
            )
        }

        var isShowingAvatar = false
        val avatarUrl = inviteDisplay.avatarUrl
        if (avatarUrl != null &&
            inviteDisplay.displayName.isNotBlank() &&
            inviteDisplay.inviteMessage.isNotBlank()
        ) {
            isShowingAvatar = true
            InviterAvatar(
                avatarUrl,
                displayName = inviteDisplay.displayName,
                inviteMessage = inviteDisplay.inviteMessage,
            )
        }

        val enterInfoTitleModifier = if (isShowingAvatar) {
            Modifier
                .listItemHorizontalPadding()
                .listItemBottomPadding()
        } else {
            listItemModifier
        }
        Text(
            t("persistentInvitations.enter_user_info"),
            enterInfoTitleModifier,
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

        BusyButton(
            fillWidthPadded,
            enabled = isEditable,
            text = t("actions.request_access"),
            indicateBusy = isJoiningOrg,
            onClick = viewModel::onVolunteerWithOrg,
        )
    }
}
