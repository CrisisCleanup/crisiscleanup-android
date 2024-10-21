package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.WorkTypeTransferType
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.LinkifyHtmlText
import com.crisiscleanup.core.designsystem.component.LinkifyPhoneEmailText
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemNestedPadding
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.TransferWorkTypeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransferWorkTypesRoute(
    onBack: () -> Unit = {},
    viewModel: TransferWorkTypeViewModel = hiltViewModel(),
) {
    val isTransferred by viewModel.isTransferred.collectAsStateWithLifecycle()
    if (isTransferred) {
        onBack()
    }

    val isTransferring by viewModel.isTransferring.collectAsStateWithLifecycle()
    val isEditable = !isTransferring
    val conditionalBack = if (isEditable) {
        onBack
    } else {
        {}
    }
    BackHandler(isEditable) {
        onBack()
    }

    if (viewModel.isTransferable) {
        val closeKeyboard = rememberCloseKeyboard(viewModel)
        val onTransfer = remember(viewModel) {
            {
                if (viewModel.commitTransfer()) {
                    closeKeyboard()
                }
            }
        }
        // TODO Janky in logs. Switch to LazyColumn?
        Column {
            TopAppBarBackAction(
                title = viewModel.screenTitle,
                onAction = conditionalBack,
            )
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .scrollFlingListener(closeKeyboard)
                    .verticalScroll(scrollState)
                    .weight(1f),
            ) {
                CompositionLocalProvider(
                    LocalAppTranslator provides viewModel,
                ) {
                    TransferWorkTypesView(
                        isEditable = isEditable,
                        onTransfer = onTransfer,
                    )
                }
            }
            BottomActionBar(
                onBack,
                isEditable,
                onTransfer,
            )
        }

        val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
        var showErrorMessage by remember(errorMessage) { mutableStateOf(true) }
        if (errorMessage.isNotBlank() && showErrorMessage) {
            val closeDialog = {
                showErrorMessage = false
                viewModel.clearErrorMessage()
            }
            CrisisCleanupAlertDialog(
                text = errorMessage,
                onDismissRequest = closeDialog,
                confirmButton = {
                    CrisisCleanupTextButton(
                        text = LocalAppTranslator.current("actions.ok"),
                        onClick = closeDialog,
                    )
                },
            )
        }
    } else {
        onBack()
    }
}

@Composable
internal fun TransferWorkTypesView(
    viewModel: TransferWorkTypeViewModel = hiltViewModel(),
    isEditable: Boolean = false,
    onTransfer: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    val textStyle = MaterialTheme.typography.bodyLarge

    when (viewModel.transferType) {
        WorkTypeTransferType.Release -> {
            Text(
                translator("caseView.please_justify_release"),
                Modifier.listItemPadding(),
                style = textStyle,
            )

            ReasonSection(viewModel, isEditable, onTransfer)

            WorkTypeSection(viewModel, isEditable)
        }

        WorkTypeTransferType.Request -> {
            val textModifier = Modifier.listItemHorizontalPadding()
            val requestDescription by viewModel.requestDescription.collectAsStateWithLifecycle()
            LinkifyHtmlText(
                requestDescription,
                textModifier.listItemTopPadding(),
            )

            val contacts by viewModel.contactList.collectAsStateWithLifecycle()
            if (contacts.isNotEmpty()) {
                Text(
                    translator("workTypeRequestModal.contacts"),
                    // TODO Common dimensions
                    textModifier,
                    style = LocalFontStyles.current.header4,
                )
                for (s in contacts) {
                    LinkifyPhoneEmailText(
                        s,
                        listItemModifier,
                    )
                }
            }

            WorkTypeSection(viewModel, isEditable)

            LinkifyHtmlText(
                translator("workTypeRequestModal.please_add_respectful_note"),
                textModifier,
            )

            val requestExamples = listOf(
                "workTypeRequestModal.reason_member_of_faith_community",
                "workTypeRequestModal.reason_working_next_door",
                "workTypeRequestModal.reason_we_did_the_work",
            ).map { translator(it) }
            requestExamples.forEachIndexed { index, s ->
                val listItemModifier =
                    if (index > 0) textModifier.listItemTopPadding() else textModifier
                Text(
                    "\u2022 $s",
                    listItemModifier.listItemNestedPadding(),
                    style = textStyle,
                )
            }

            ReasonSection(viewModel, isEditable, onTransfer)
        }

        else -> {}
    }
}

@Composable
private fun ReasonSection(
    viewModel: TransferWorkTypeViewModel,
    isEditable: Boolean = false,
    onTransfer: () -> Unit = {},
) {
    val errorMessageReason by viewModel.errorMessageReason.collectAsStateWithLifecycle()
    val hasFocus = errorMessageReason.isNotEmpty()

    CrisisCleanupTextArea(
        text = viewModel.transferReason,
        onTextChange = { viewModel.transferReason = it },
        placeholder = viewModel.reasonHint?.let { reasonHint ->
            { Text(reasonHint) }
        },
        modifier = Modifier
            .listItemPadding()
            .fillMaxWidth(),
        onDone = { onTransfer() },
        enabled = isEditable,
        hasFocus = hasFocus,
    )
}

@Composable
private fun WorkTypeSection(
    viewModel: TransferWorkTypeViewModel,
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    // TODO Focus on first checkbox if work type error exists

    val workTypeState = remember { viewModel.workTypesState }
    val checkboxItemModifier = listItemModifier.listCheckboxAlignStartOffset()
    viewModel.sortedWorkTypes.forEach { workType ->
        val id = workType.id
        val isChecked = workTypeState[id]!!
        val updateRequests = { b: Boolean ->
            workTypeState[id] = b
            viewModel.updateRequestInfo()
        }
        CrisisCleanupTextCheckbox(
            checkboxItemModifier,
            isChecked,
            text = translator(workType.workTypeLiteral),
            onToggle = { updateRequests(!isChecked) },
            onCheckChange = updateRequests,
            enabled = isEditable,
        )
    }
}

@Composable
private fun BottomActionBar(
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
    onTransfer: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    Row(
        modifier = Modifier
            .padding(16.dp),
        horizontalArrangement = listItemSpacedBy,
    ) {
        BusyButton(
            Modifier.weight(1f),
            text = translator("actions.cancel"),
            enabled = isEditable,
            onClick = onBack,
            colors = cancelButtonColors(),
        )
        BusyButton(
            Modifier.weight(1f),
            text = translator("actions.ok"),
            enabled = isEditable,
            onClick = onTransfer,
        )
    }
}
