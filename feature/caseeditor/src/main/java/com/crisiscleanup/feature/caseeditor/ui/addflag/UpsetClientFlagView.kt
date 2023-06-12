package com.crisiscleanup.feature.caseeditor.ui.addflag

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.model.data.IncidentOrganization
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.caseeditor.CaseAddFlagViewModel

@Composable
internal fun ColumnScope.UpsetClientView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current.translator

    val closeKeyboard = rememberCloseKeyboard(viewModel)

//    val organizations by viewModel.searchOrganizations.collectAsStateWithLifecycle()
    var flagNotes by remember { mutableStateOf("") }
    var organizationInvolvement by remember { mutableStateOf<Boolean?>(null) }
    val updateInvolvement = remember(viewModel) { { b: Boolean -> organizationInvolvement = b } }
    var otherOrganizations by remember { mutableStateOf<List<IncidentOrganization>>(emptyList()) }

    LazyColumn(
        Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        labelTextItem(translator("flag.explain_why_client_upset"))

        item {
            TextArea(
                flagNotes,
                { text: String -> flagNotes = text },
                listItemModifier,
                isEditable = isEditable,
                onDone = closeKeyboard,
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
            var organizationName by remember { mutableStateOf("") }
            val updateName = { s: String -> organizationName = s }
            Box(Modifier.fillMaxWidth()) {
                var contentWidth by remember { mutableStateOf(Size.Zero) }

                OutlinedClearableTextField(
                    modifier = listItemModifier.onGloballyPositioned {
                        contentWidth = it.size.toSize()
                    },
                    labelResId = 0,
                    label = translator("profileOrg.organization_name"),
                    value = organizationName,
                    onValueChange = updateName,
                    keyboardType = KeyboardType.Text,
                    keyboardCapitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                    isError = false,
                    hasFocus = false,
                    onEnter = closeKeyboard,
                    enabled = isEditable,
                )
            }
        }
    }

    AddFlagSaveActionBar(
        onSave = { viewModel.onUpsetClient(flagNotes) },
        onCancel = onBack,
        isEditable = isEditable,
    )
}