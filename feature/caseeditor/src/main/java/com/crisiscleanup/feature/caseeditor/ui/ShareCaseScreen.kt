package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.LeadingIconChip
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.TopAppBarCancelAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.optionItemPadding
import com.crisiscleanup.core.designsystem.theme.primaryRedColor
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.rememberIsKeyboardOpen
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseShareViewModel
import com.crisiscleanup.feature.caseeditor.ShareContactInfo
import com.crisiscleanup.feature.caseeditor.util.CaseStaticText
import com.crisiscleanup.feature.caseeditor.util.TwoActionBar
import com.crisiscleanup.feature.caseeditor.util.labelTextItem
import com.crisiscleanup.feature.caseeditor.util.listTextItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseEditShareCaseRoute(
    onBack: () -> Unit = {},
    viewModel: CaseShareViewModel = hiltViewModel(),
) {
    val isShared by viewModel.isShared.collectAsStateWithLifecycle()
    if (isShared) {
        onBack()
    } else {
        val hasClaimedWorkType by viewModel.hasClaimedWorkType.collectAsStateWithLifecycle()
        val isOnSecondStep = viewModel.showShareScreen && hasClaimedWorkType != true
        val navigateBack = remember(isOnSecondStep) {
            {
                if (isOnSecondStep) {
                    viewModel.showShareScreen = false
                } else {
                    onBack()
                }
            }
        }
        BackHandler {
            navigateBack()
        }

        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val isSharing by viewModel.isSharing.collectAsStateWithLifecycle()

        val notSharableMessage by viewModel.notSharableMessage.collectAsStateWithLifecycle("")

        val isEditable = !(isLoading || isSharing) && notSharableMessage.isBlank()

        val translator: KeyResourceTranslator = viewModel.translator
        CompositionLocalProvider(
            LocalAppTranslator provides translator,
        ) {
            Column {
                val screenTitle = translator("actions.share")
                if (isOnSecondStep) {
                    TopAppBarBackAction(
                        title = screenTitle,
                        onAction = navigateBack,
                    )
                } else {
                    TopAppBarCancelAction(
                        title = screenTitle,
                        onAction = navigateBack,
                    )
                }

                if (notSharableMessage.isNotBlank()) {
                    CompositionLocalProvider(LocalContentColor provides primaryRedColor) {
                        Text(
                            notSharableMessage,
                            listItemModifier,
                            style = LocalFontStyles.current.header5,
                        )
                    }
                }

                CaseShareContent(
                    isLoading = isLoading,
                    isSharing = isSharing,
                    isEditable = isEditable,
                    onBack = navigateBack,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CaseShareContent(
    isLoading: Boolean,
    isSharing: Boolean,
    isEditable: Boolean,
    onBack: () -> Unit,
    viewModel: CaseShareViewModel = hiltViewModel(),
) {
    val translator = LocalAppTranslator.current
    val closeKeyboard = rememberCloseKeyboard(viewModel)

    val isShareEnabled by viewModel.isShareEnabled.collectAsStateWithLifecycle(true)
    val hasClaimedWorkType by viewModel.hasClaimedWorkType.collectAsStateWithLifecycle()
    val contactErrorMessage = viewModel.contactErrorMessage
    val receiverContacts by viewModel.receiverContacts.collectAsStateWithLifecycle()
    val onRemoveContact = remember(viewModel) { { index: Int -> viewModel.deleteContact(index) } }

    Box(Modifier.weight(1f)) {
        LazyColumn(
            modifier = Modifier
                .scrollFlingListener(closeKeyboard)
                .fillMaxSize(),
        ) {
            if (viewModel.showShareScreen) {
                shareCaseInput(
                    viewModel,
                    closeKeyboard,
                    translator,
                    isEditable = isEditable,
                    contactErrorMessage = contactErrorMessage,
                    receiverContacts = receiverContacts,
                    onRemoveContact = onRemoveContact,
                )
            } else {
                unclaimedShare(
                    viewModel,
                    translator,
                    isEditable = isEditable,
                    hasClaimedWorkType,
                    closeKeyboard,
                    viewModel::continueToShareScreen,
                    onBack,
                )
            }
        }

        AnimatedBusyIndicator(isLoading)
    }

    val isKeyboardOpen = rememberIsKeyboardOpen()
    if (viewModel.showShareScreen && !isKeyboardOpen) {
        TwoActionBar(
            onCancel = onBack,
            enabled = isEditable,
            enablePositive = isShareEnabled,
            positiveTranslationKey = "actions.share",
            onPositiveAction = viewModel::onShare,
            isBusy = isSharing,
        )
    }
}

private fun LazyListScope.unclaimedShare(
    viewModel: CaseShareViewModel,
    translator: KeyResourceTranslator,
    isEditable: Boolean,
    hasClaimedWorkType: Boolean?,
    closeKeyboard: () -> Unit = {},
    onContinueSharing: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    labelTextItem(translator("casesVue.please_claim_if_share"))
    item {
        CrisisCleanupTextArea(
            viewModel.unclaimedShareReason,
            { text: String -> viewModel.unclaimedShareReason = text },
            listItemModifier,
            enabled = isEditable,
            imeAction = ImeAction.Done,
            onDone = closeKeyboard,
        )
    }

    if (hasClaimedWorkType == false) {
        item {
            CrisisCleanupButton(
                listItemModifier,
                text = translator("actions.share_no_claim"),
                onClick = onContinueSharing,
                enabled = isEditable && viewModel.unclaimedShareReason.isNotBlank(),
            )
        }
    }

    item {
        CrisisCleanupButton(
            listItemModifier,
            text = translator("actions.claim_and_share"),
            onClick = onContinueSharing,
            enabled = isEditable,
        )
    }

    item {
        CrisisCleanupButton(
            listItemModifier,
            text = translator("actions.cancel"),
            colors = cancelButtonColors(),
            onClick = onCancel,
        )
    }
}

private fun LazyListScope.shareCaseInput(
    viewModel: CaseShareViewModel,
    closeKeyboard: () -> Unit = {},
    translator: KeyResourceTranslator,
    isEditable: Boolean = false,
    contactErrorMessage: String = "",
    receiverContacts: List<ShareContactInfo> = emptyList(),
    onRemoveContact: (Int) -> Unit = {},
) {
    listTextItem(translator("shareWorksite.share_via_email_phone_intro"))

    labelTextItem(
        translator("shareWorksite.share_case_method"),
        isBold = true,
    )

    item {
        CrisisCleanupRadioButton(
            listItemModifier.listCheckboxAlignStartOffset(),
            viewModel.isEmailContactMethod,
            text = translator("shareWorksite.email"),
            onSelect = { viewModel.isEmailContactMethod = true },
            enabled = isEditable,
        )
    }
    item {
        CrisisCleanupRadioButton(
            listItemModifier.listCheckboxAlignStartOffset(),
            !viewModel.isEmailContactMethod,
            text = translator("shareWorksite.sms_text_message"),
            onSelect = { viewModel.isEmailContactMethod = false },
            enabled = isEditable,
        )
    }

    if (receiverContacts.isNotEmpty()) {
        item {
            ReceiverContactItem(
                receiverContacts,
                translator,
                isEditable,
                onRemoveContact,
            )
        }
    }

    if (contactErrorMessage.isNotBlank()) {
        item {
            CompositionLocalProvider(LocalContentColor provides primaryRedColor) {
                CaseStaticText(
                    modifier = listItemModifier,
                    text = contactErrorMessage,
                )
            }
        }
    }

    item {
        val hintTranslationKey = if (viewModel.isEmailContactMethod) {
            "shareWorksite.manually_enter_emails"
        } else {
            "shareWorksite.manually_enter_phones"
        }
        val receiverContact by viewModel.receiverContactManual.collectAsStateWithLifecycle()
        Row(
            listItemModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = listItemSpacedByHalf,
        ) {
            val acceptInput = {
                if (receiverContact.isBlank()) {
                    closeKeyboard()
                } else {
                    viewModel.onAddContact(receiverContact)
                }
            }
            OutlinedClearableTextField(
                modifier = Modifier.weight(1f),
                labelResId = 0,
                label = translator(hintTranslationKey),
                value = receiverContact,
                onValueChange = { viewModel.receiverContactManual.value = it },
                keyboardType = if (viewModel.isEmailContactMethod) KeyboardType.Email else KeyboardType.Password,
                imeAction = ImeAction.Done,
                isError = false,
                hasFocus = false,
                onEnter = acceptInput,
                enabled = isEditable,
            )

            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.Check,
                onClick = acceptInput,
                enabled = isEditable,
            )
        }
    }

    contactSuggestionsItem(
        viewModel,
        closeKeyboard,
        translator,
        isEditable,
    )

    listTextItem(translator("shareWorksite.add_message"))
    item {
        CrisisCleanupTextArea(
            viewModel.receiverMessage,
            { text: String -> viewModel.receiverMessage = text },
            listItemModifier,
            enabled = isEditable,
            imeAction = ImeAction.Done,
            onDone = closeKeyboard,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiverContactItem(
    receiverContacts: List<ShareContactInfo>,
    translator: KeyResourceTranslator,
    isEditable: Boolean,
    onRemoveContact: (Int) -> Unit,
) {
    var contentColor = Color.Black
    if (!isEditable) {
        contentColor = contentColor.disabledAlpha()
    }

    val removeShareTranslateKey = "shareWorksite.remove_share_user"
    val removeShareStringTemplate = translator(removeShareTranslateKey)

    FlowRow(
        listItemModifier,
        horizontalArrangement = listItemSpacedBy,
    ) {
        receiverContacts.forEachIndexed { index, contact ->
            val description = removeShareStringTemplate.replace("{user}", contact.contactValue)

            LeadingIconChip(
                contact.contactValue,
                { onRemoveContact(index) },
                isEditable,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                iconDescription = description,
                contentTint = contentColor,
            )
        }
    }
}

private fun LazyListScope.contactSuggestionsItem(
    viewModel: CaseShareViewModel,
    closeKeyboard: () -> Unit = {},
    translator: KeyResourceTranslator,
    isEditable: Boolean = false,
) {
    item {
        val contactOptions by viewModel.contactOptions.collectAsStateWithLifecycle()
        val receiverContact by viewModel.receiverContactSuggestion.collectAsStateWithLifecycle()
        val isEmail = viewModel.isEmailContactMethod
        val searchForHintTranslationKey =
            if (isEmail) "shareWorksite.search_emails" else "shareWorksite.search_phones"
        val keyboardType = if (isEmail) KeyboardType.Email else KeyboardType.Password
        Box(Modifier.fillMaxWidth()) {
            var contentSize by remember { mutableStateOf(Size.Zero) }

            var dismissSuggestionsQuery by remember { mutableStateOf("") }
            OutlinedClearableTextField(
                modifier = listItemModifier.onGloballyPositioned {
                    contentSize = it.size.toSize()
                },
                labelResId = 0,
                label = translator(searchForHintTranslationKey),
                value = receiverContact,
                onValueChange = { viewModel.receiverContactSuggestion.value = it },
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
                isError = false,
                hasFocus = false,
                onEnter = {
                    if (receiverContact.isBlank()) {
                        closeKeyboard()
                    }
                },
                enabled = isEditable,
            )

            val dismissDropdown =
                remember(viewModel) { { dismissSuggestionsQuery = receiverContact } }
            val showDropdown by remember(
                contactOptions,
                dismissSuggestionsQuery,
                receiverContact,
            ) {
                derivedStateOf {
                    contactOptions.isNotEmpty() && receiverContact.isNotBlank() && dismissSuggestionsQuery != receiverContact
                }
            }
            DropdownMenu(
                modifier = Modifier
                    .width(with(LocalDensity.current) { contentSize.width.toDp() })
                    // TODO Use inner window height - approximate keyboard height
                    .heightIn(max = 300.dp),
                expanded = showDropdown,
                onDismissRequest = dismissDropdown,
                offset = listItemDropdownMenuOffset,
                properties = PopupProperties(focusable = false),
            ) {
                BackHandler {
                    dismissDropdown()
                }
                contactOptions.forEach { contact ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${contact.name}\n${contact.contactValue}",
                                Modifier.optionItemPadding(),
                                style = LocalFontStyles.current.header4,
                            )
                        },
                        onClick = { viewModel.onAddContact(contact) },
                    )
                }
            }
        }
    }
}
