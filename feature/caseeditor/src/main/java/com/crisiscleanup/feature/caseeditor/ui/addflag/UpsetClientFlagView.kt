package com.crisiscleanup.feature.caseeditor.ui.addflag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextArea
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.optionItemPadding
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

    val closeKeyboard = rememberCloseKeyboard(viewModel)

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
            .fillMaxWidth()
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
            Box(Modifier.fillMaxWidth()) {
                var contentWidth by remember { mutableStateOf(Size.Zero) }

                var dismissSuggestionsQuery by remember { mutableStateOf("") }
                OutlinedClearableTextField(
                    modifier = listItemModifier.onGloballyPositioned {
                        contentWidth = it.size.toSize()
                    },
                    labelResId = 0,
                    label = translator("profileOrg.organization_name"),
                    value = otherOrgQuery,
                    onValueChange = {
                        dismissSuggestionsQuery = ""
                        onOtherOrgQueryChange(it)
                    },
                    keyboardType = KeyboardType.Text,
                    keyboardCapitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                    isError = false,
                    hasFocus = false,
                    onEnter = closeKeyboard,
                    enabled = isEditable,
                )

                var selectedOptionQuery by remember { mutableStateOf("") }
                val dismissDropdown =
                    remember(viewModel) { { dismissSuggestionsQuery = otherOrgQuery } }
                val showDropdown by remember(
                    otherOrgSuggestions,
                    dismissSuggestionsQuery,
                    selectedOptionQuery,
                    otherOrgQuery,
                ) {
                    derivedStateOf {
                        otherOrgSuggestions.isNotEmpty() &&
                                dismissSuggestionsQuery != otherOrgQuery &&
                                selectedOptionQuery != otherOrgQuery
                    }
                }
                DropdownMenu(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { contentWidth.width.toDp() })
                        // TODO Use inner window height - approximate keyboard height
                        .heightIn(max = 300.dp),
                    expanded = showDropdown,
                    onDismissRequest = dismissDropdown,
                    offset = listItemDropdownMenuOffset,
                    properties = PopupProperties(focusable = false)
                ) {
                    BackHandler {
                        dismissDropdown()
                    }
                    otherOrgSuggestions.forEach { organization ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    organization.name,
                                    Modifier.optionItemPadding(),
                                )
                            },
                            onClick = {
                                selectedOptionQuery = organization.name
                                onOtherOrgQueryChange(organization.name)
                                otherOrganizations = listOf(organization)
                            },
                        )
                    }
                }
            }
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