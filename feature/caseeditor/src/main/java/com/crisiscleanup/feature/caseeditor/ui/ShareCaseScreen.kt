package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.designsystem.AppTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.TopAppBarCancelAction
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.caseeditor.CaseShareViewModel
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.util.TwoActionBar
import com.crisiscleanup.feature.caseeditor.util.labelTextItem
import com.crisiscleanup.feature.caseeditor.util.listTextItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseEditShareCaseRoute(
    onBack: () -> Unit = {},
    viewModel: CaseShareViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    val appTranslator = remember(viewModel) {
        AppTranslator(translator = translator)
    }

    val closeKeyboard = rememberCloseKeyboard(viewModel)

    CompositionLocalProvider(
        LocalAppTranslator provides appTranslator,
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBarCancelAction(
                title = translator("actions.share"),
                onAction = onBack,
            )

            // TODO Show message and disable share if internet connection is not available/token is invalid?

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                unclaimedShare(
                    closeKeyboard,
                    translator,
                )

                shareCaseInput(
                    closeKeyboard,
                    translator,
                    // TODO From view model
                    isEditable = true,
                )
            }

            TwoActionBar(
                positiveTranslationKey = "actions.share",
            )
        }
    }
}

private fun LazyListScope.unclaimedShare(
    closeKeyboard: () -> Unit = {},
    translator: KeyResourceTranslator,
) {
    labelTextItem(translator("casesVue.please_claim_if_share"))
    // TODO Move into view model
    item {
        var unclaimedReason by remember { mutableStateOf("") }
        CrisisCleanupTextArea(
            unclaimedReason,
            { text: String -> unclaimedReason = text },
            listItemModifier,
            enabled = true,
            imeAction = ImeAction.Done,
            onDone = closeKeyboard,
        )
    }
}

private fun LazyListScope.shareCaseInput(
    closeKeyboard: () -> Unit = {},
    translator: KeyResourceTranslator,
    isEditable: Boolean = false,
) {
    listTextItem(translator("shareWorksite.share_via_email_phone_intro"))

    labelTextItem(
        translator("info.share_case_method", R.string.share_case_method),
        isBold = true,
    )

    item {
        CrisisCleanupRadioButton(
            listItemModifier.listCheckboxAlignStartOffset(),
            // TODO Use and update view model var
            false,
            text = translator("formLabels.email"),
            onSelect = { },
            enabled = isEditable,
        )
    }
    item {
        CrisisCleanupRadioButton(
            listItemModifier.listCheckboxAlignStartOffset(),
            // TODO Use and update view model var
            true,
            text = translator("formLabels.sms_text_message", R.string.sms_text_message),
            onSelect = { },
            enabled = isEditable,
        )
    }

    // TODO Item row of chips
    // TODO Text input with suggestions. Done submits text entered with validator

    listTextItem(translator("shareWorksite.add_message"))
    item {
        var receiverMessage by remember { mutableStateOf("") }
        CrisisCleanupTextArea(
            receiverMessage,
            { text: String -> receiverMessage = text },
            listItemModifier,
            enabled = true,
            imeAction = ImeAction.Done,
            onDone = closeKeyboard,
        )
    }
}