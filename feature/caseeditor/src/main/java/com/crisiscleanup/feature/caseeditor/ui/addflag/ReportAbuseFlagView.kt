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
import com.crisiscleanup.feature.caseeditor.util.listTextItem

@Composable
internal fun ColumnScope.ReportAbuseFlagView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current

    val closeKeyboard = rememberCloseKeyboard(viewModel)

    val otherOrgQuery by viewModel.otherOrgQ.collectAsStateWithLifecycle()
    val onOtherOrgQueryChange = remember(viewModel) {
        { q: String -> viewModel.onOrgQueryChange(q) }
    }
    var otherOrganizations by remember { mutableStateOf<List<OrganizationIdName>>(emptyList()) }
    val otherOrgSuggestions by viewModel.otherOrgResults.collectAsStateWithLifecycle()

    var outcome by remember { mutableStateOf("") }
    var flagNotes by remember { mutableStateOf("") }
    var flagAction by remember { mutableStateOf("") }
    var organizationContacted by remember { mutableStateOf<Boolean?>(null) }
    val updateContacted = remember(viewModel) { { b: Boolean -> organizationContacted = b } }

    LazyColumn(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .weight(1f)
            .fillMaxWidth(),
    ) {
        listTextItem(translator("flag.organizations_complaining_about"))

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

        listTextItem(translator("flag.must_contact_org_first"))

        labelTextItem(translator("flag.have_you_contacted_org"))
        val radioModifier = Modifier
            .fillMaxWidth()
            .listItemHeight()
            .listItemPadding()
        item {
            CrisisCleanupRadioButton(
                radioModifier,
                organizationContacted == true,
                text = translator("formOptions.yes"),
                onSelect = { updateContacted(true) },
                enabled = isEditable,
            )
        }
        item {
            CrisisCleanupRadioButton(
                radioModifier,
                organizationContacted == false,
                text = translator("formOptions.no"),
                onSelect = { updateContacted(false) },
                enabled = isEditable,
            )
        }

        labelTextItem(translator("flag.outcome_of_contact"))
        item {
            CrisisCleanupTextArea(
                outcome,
                { text: String -> outcome = text },
                listItemModifier,
                enabled = isEditable,
                imeAction = ImeAction.Next,
            )
        }

        labelTextItem(translator("flag.describe_problem"))
        item {
            CrisisCleanupTextArea(
                flagNotes,
                { text: String -> flagNotes = text },
                listItemModifier,
                enabled = isEditable,
                imeAction = ImeAction.Next,
            )
        }

        labelTextItem(translator("flag.suggested_outcome"))
        item {
            CrisisCleanupTextArea(
                flagAction,
                { text: String -> flagAction = text },
                listItemModifier,
                enabled = isEditable,
                onDone = closeKeyboard,
            )
        }

        listTextItem(translator("flag.warning_ccu_cannot_do_much"))
    }

    val onSave = remember(viewModel) {
        {
            viewModel.onReportAbuse(
                organizationContacted,
                outcome,
                flagNotes,
                flagAction,
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
