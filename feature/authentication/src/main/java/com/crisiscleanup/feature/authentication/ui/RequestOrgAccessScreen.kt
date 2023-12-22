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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import com.crisiscleanup.core.designsystem.theme.primaryRedColor
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.authentication.RequestOrgAccessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestOrgAccessRoute(
    onBack: () -> Unit,
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val clearStateOnBack = remember(onBack, viewModel) {
        {
            viewModel.clearInviteCode()

            onBack()
        }
    }
    // TODO Backing out does nothing when directed from paste invite link
    BackHandler {
        clearStateOnBack()
    }

    val screenTitle by viewModel.screenTitle.collectAsStateWithLifecycle()

    val inviteInfoErrorMessage by viewModel.inviteInfoErrorMessage.collectAsStateWithLifecycle()
    val isInviteRequested by viewModel.isInviteRequested.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBarBackAction(
            title = screenTitle,
            onAction = clearStateOnBack,
        )

        if (inviteInfoErrorMessage.isNotBlank()) {
            Text(
                inviteInfoErrorMessage,
                listItemModifier,
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

    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (viewModel.showEmailInput) {
            val requestInstructions = t("requestAccess.request_access_enter_email")
            Text(
                requestInstructions,
                listItemModifier,
            )

            // TODO Set initial focus

            val hasEmailError = viewModel.emailAddressError.isNotBlank()
            if (hasEmailError) {
                Text(
                    viewModel.emailAddressError,
                    Modifier
                        .listItemHorizontalPadding()
                        .listItemTopPadding(),
                    color = primaryRedColor,
                )
            }
            OutlinedClearableTextField(
                modifier = listItemModifier,
                label = t("requestAccess.existing_member_email"),
                value = viewModel.emailAddress,
                onValueChange = { viewModel.emailAddress = it },
                keyboardType = KeyboardType.Email,
                enabled = isEditable,
                isError = hasEmailError,
                hasFocus = hasEmailError,
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
                            contentDescription = info.displayName,
                            fallback = fallbackPainter,
                            contentScale = ContentScale.FillBounds,
                            placeholder = placeholderPainter,
                        )

                        Column {
                            Text(
                                info.displayName,
                                style = LocalFontStyles.current.header4,
                            )
                            Text(
                                info.inviteMessage,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        Text(
            t("requestAccess.complete_form_request_access"),
            fillWidthPadded,
            style = LocalFontStyles.current.header3,
        )

        val languageOptions by viewModel.languageOptions.collectAsStateWithLifecycle()
        UserInfoInputView(
            infoData = viewModel.userInfo,
            languageOptions = languageOptions,
            isEditable = isEditable,
        )

        Text(
            t("requestAccess.request_will_be_sent"),
            listItemModifier,
        )

        BusyButton(
            fillWidthPadded,
            enabled = isEditable,
            text = t("actions.request_access"),
            indicateBusy = isLoading,
            onClick = viewModel::onVolunteerWithOrg,
        )
    }
}
