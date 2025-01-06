package com.crisiscleanup.feature.caseeditor.ui.addflag

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.model.data.OrganizationIdName
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseAddFlagViewModel
import com.crisiscleanup.feature.caseeditor.util.labelTextItem

@Composable
internal fun ColumnScope.UpsetClientFlagView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    val closeKeyboard = rememberCloseKeyboard()

    var flagNotes by remember { mutableStateOf("") }
    var organizationInvolvement by remember { mutableStateOf<Boolean?>(null) }
    val updateInvolvement = remember(viewModel) { { b: Boolean -> organizationInvolvement = b } }

    val otherOrgQuery by viewModel.otherOrgQ.collectAsStateWithLifecycle()
    val onOtherOrgQueryChange = remember(viewModel) {
        { q: String -> viewModel.onOrgQueryChange(q) }
    }
    var otherOrganizations by remember { mutableStateOf<List<OrganizationIdName>>(emptyList()) }
    val otherOrgSuggestions by viewModel.otherOrgResults.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .weight(1f)
            .fillMaxWidth(),
    ) {
        labelTextItem(translator("flag.explain_why_client_upset"))

        item {
            CrisisCleanupTextArea(
                flagNotes,
                { text: String -> flagNotes = text },
                listItemModifier,
                enabled = isEditable,
                imeAction = ImeAction.Next,
            )
        }

        labelTextItem(translator("flag.does_issue_involve_you"))
        val radioModifier = Modifier
            .fillMaxWidth()
            .listItemHeight()
            .listItemPadding()
        item {
            CrisisCleanupRadioButton(
                radioModifier,
                organizationInvolvement == true,
                text = translator("formOptions.yes"),
                onSelect = { updateInvolvement(true) },
                enabled = isEditable,
            )
        }
        item {
            CrisisCleanupRadioButton(
                radioModifier,
                organizationInvolvement == false,
                text = translator("formOptions.no"),
                onSelect = { updateInvolvement(false) },
                enabled = isEditable,
            )
        }

        labelTextItem(translator("flag.please_share_other_orgs"))

        item {
            OrganizationsSearch(
                orgQuery = otherOrgQuery,
                onQueryChange = onOtherOrgQueryChange,
                orgSuggestions = otherOrgSuggestions,
                onOrgSelected = { otherOrganizations = listOf(it) },
                rememberKey = viewModel,
                isEditable = isEditable,
            )
        }
    }

    val onSave = remember(viewModel) {
        {
            viewModel.onUpsetClient(
                flagNotes,
                organizationInvolvement,
                otherOrgQuery,
                otherOrganizations,
            )
        }
    }
    AddFlagSaveActionBar(
        onSave = onSave,
        onCancel = onBack,
        enabled = isEditable,
    )
}
