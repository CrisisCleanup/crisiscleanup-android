package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.RegisterSuccessView
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.RequestOrgAccessViewModel
import kotlinx.coroutines.launch
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestOrgAccessRoute(
    onBack: () -> Unit,
    closeRequestAccess: () -> Unit,
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val isInviteRequested by viewModel.isInviteRequested.collectAsStateWithLifecycle()

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

        if (inviteInfoErrorMessage.isNotBlank()) {
            Text(
                inviteInfoErrorMessage,
                listItemModifier.testTag("requestAccessInviteInfoError"),
                style = LocalFontStyles.current.header3,
            )
        } else if (isInviteRequested) {
            RegisterSuccessView(
                title = viewModel.requestSentTitle,
                text = viewModel.requestSentText,
            )
        } else {
            RequestOrgUserInfoInputView()
        }
    }
}

@Composable
private fun RequestOrgUserInfoInputView(
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current
    val closeKeyboard = rememberCloseKeyboard(viewModel)

    val displayInfo by viewModel.inviteDisplay.collectAsStateWithLifecycle()

    val isEditable by viewModel.isEditable.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val clearErrorVisuals = remember(viewModel) { { viewModel.clearErrors() } }

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
                val info = displayInfo!!
                val avatarUrl = displayInfo?.avatarUrl
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

        BusyButton(
            fillWidthPadded.testTag("requestAccessSubmitAction"),
            enabled = isEditable,
            text = t("actions.request_access"),
            indicateBusy = isLoading,
            onClick = viewModel::onVolunteerWithOrg,
        )
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
