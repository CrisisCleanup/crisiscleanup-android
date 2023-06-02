package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding
import com.crisiscleanup.core.designsystem.theme.textBoxHeight
import com.crisiscleanup.core.ui.LinkifyHtmlText
import com.crisiscleanup.core.ui.LinkifyPhoneEmailText
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.TransferWorkTypeViewModel
import com.crisiscleanup.feature.caseeditor.WorkTypeTransferType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransferWorkTypesRoute(
    viewModel: TransferWorkTypeViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val isTransferred by viewModel.isTransferred.collectAsStateWithLifecycle()
    if (isTransferred) {
        onBack()
    }

    val isTransferring by viewModel.isTransferring.collectAsStateWithLifecycle()
    val isEditable = !isTransferring
    val conditionalBack = if (isEditable) onBack else {
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
                    .weight(1f)
            ) {
                TransferWorkTypesView(
                    isEditable = isEditable,
                    onTransfer = onTransfer,
                )
            }
            BottomActionBar(
                viewModel,
                onBack,
                isEditable,
                onTransfer,
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
    val translate = remember(viewModel) { { s: String -> viewModel.translate(s) } }
    val textStyle = MaterialTheme.typography.bodyMedium

    when (viewModel.transferType) {
        WorkTypeTransferType.Release -> {
            Text(
                translate("caseView.please_justify_release"),
                Modifier.listItemPadding(),
                style = textStyle,
            )

            ReasonSection(viewModel, isEditable, onTransfer)

            WorkTypeSection(viewModel, translate, isEditable)
        }

        WorkTypeTransferType.Request -> {
            val textModifier = Modifier.listItemHorizontalPadding()
            val requestDescription by viewModel.requestDescription.collectAsStateWithLifecycle()
            LinkifyHtmlText(
                requestDescription,
                textModifier.listItemTopPadding(),
                style = textStyle,
            )

            LinkifyHtmlText(
                translate("workTypeRequestModal.please_add_respectful_note"),
                textModifier,
                style = textStyle,
            )

            val requestExamples = listOf(
                "workTypeRequestModal.reason_member_of_faith_community",
                "workTypeRequestModal.reason_working_next_door",
                "workTypeRequestModal.reason_we_did_the_work",
            )
            requestExamples.forEachIndexed { index, s ->
                Text(
                    "\u2022 ${translate(s)}",
                    if (index > 0) textModifier.listItemTopPadding() else textModifier,
                    style = textStyle,
                )
            }

            val contacts by viewModel.contactList.collectAsStateWithLifecycle()
            if (contacts.isNotEmpty()) {
                Text(
                    translate("workTypeRequestModal.contacts"),
                    // TODO Common dimensions
                    textModifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                for (s in contacts) {
                    LinkifyPhoneEmailText(
                        s,
                        listItemModifier,
                        textStyle,
                    )
                }
            }

            WorkTypeSection(viewModel, translate, isEditable)

            ReasonSection(viewModel, isEditable, onTransfer)
        }

        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonSection(
    viewModel: TransferWorkTypeViewModel,
    isEditable: Boolean = false,
    onTransfer: () -> Unit = {},
) {

    val errorMessageReason by viewModel.errorMessageReason.collectAsStateWithLifecycle()
    ErrorText(errorMessageReason)

    val hasFocus = errorMessageReason.isNotEmpty()

    val keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Done,
        keyboardType = KeyboardType.Password,
    )
    val keyboardActions = KeyboardActions(
        onDone = { onTransfer() },
    )
    val focusRequester = FocusRequester()
    OutlinedTextField(
        viewModel.transferReason,
        onValueChange = { viewModel.transferReason = it },
        modifier = Modifier
            .listItemPadding()
            .fillMaxWidth()
            .textBoxHeight()
            .focusRequester(focusRequester),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = isEditable,
        placeholder = viewModel.reasonHint?.let { reasonHint ->
            { Text(reasonHint) }
        },
    )

    if (hasFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun WorkTypeSection(
    viewModel: TransferWorkTypeViewModel,
    translate: (String) -> String = { s -> s },
    isEditable: Boolean = false,
) {
    val errorMessageWorkType by viewModel.errorMessageWorkType.collectAsStateWithLifecycle()
    ErrorText(errorMessageWorkType)

    val workTypeState = remember { viewModel.workTypesState }
    val checkboxItemModifier = listItemModifier.listCheckboxAlignStartOffset()
    viewModel.transferWorkTypesState.forEach {
        val workType = it.key
        val id = workType.id
        val isChecked = workTypeState[id]!!
        val updateRequests = { b: Boolean ->
            workTypeState[id] = b
            viewModel.updateRequestInfo()
        }
        CrisisCleanupTextCheckbox(
            checkboxItemModifier,
            isChecked,
            text = translate(workType.workTypeLiteral),
            onToggle = { updateRequests(!isChecked) },
            onCheckChange = updateRequests,
            enabled = isEditable && workTypeState.size > 1,
        )
    }
}

@Composable
private fun BottomActionBar(
    viewModel: TransferWorkTypeViewModel,
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
    onTransfer: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .padding(16.dp),
        horizontalArrangement = listItemSpacedBy,
    ) {
        BusyButton(
            Modifier.weight(1f),
            text = viewModel.translate("actions.cancel"),
            enabled = isEditable,
            onClick = onBack,
            colors = cancelButtonColors(),
        )
        BusyButton(
            Modifier.weight(1f),
            text = viewModel.translate("actions.ok"),
            enabled = isEditable,
            onClick = onTransfer,
        )
    }
}