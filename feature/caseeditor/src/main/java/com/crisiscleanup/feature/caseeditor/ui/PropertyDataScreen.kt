package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.commoncase.ui.ExistingCaseLocationsDropdownItems
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemNestedPadding
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.caseeditor.CasePropertyDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun PropertyFormView(
    viewModel: EditCaseBaseViewModel,
    editor: CasePropertyDataEditor,
    focusOnOpen: Boolean = false,
    translate: (String) -> String = { s -> s },
) {
    val isEditable = LocalCaseEditor.current.isEditable

    val inputData = editor.propertyInputData

    PropertyFormResidentNameView(editor, isEditable, focusOnOpen, translate)

    // TODO Apply mask with dashes if input is purely numbers (and dashes)
    val updatePhone = remember(inputData) { { s: String -> inputData.phoneNumber = s } }
    val clearPhoneError = remember(inputData) { { inputData.phoneNumberError = "" } }
    val isPhoneError = inputData.phoneNumberError.isNotEmpty()
    val focusPhone = isPhoneError
    val phone1Label = translate("formLabels.phone1")
    ErrorText(inputData.phoneNumberError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = "$phone1Label *",
        value = inputData.phoneNumber,
        onValueChange = updatePhone,
        keyboardType = KeyboardType.Password,
        isError = isPhoneError,
        hasFocus = focusPhone,
        onNext = clearPhoneError,
        enabled = isEditable,
    )

    val updateAdditionalPhone = remember(inputData) {
        { s: String -> inputData.phoneNumberSecondary = s }
    }
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = translate("formLabels.phone2"),
        value = inputData.phoneNumberSecondary,
        onValueChange = updateAdditionalPhone,
        keyboardType = KeyboardType.Password,
        isError = false,
        enabled = isEditable,
    )

    val updateEmail = remember(inputData) { { s: String -> inputData.email = s } }
    val clearEmailError = remember(inputData) { { inputData.emailError = "" } }
    val isEmailError = inputData.emailError.isNotEmpty()
    val focusEmail = isEmailError
    val closeKeyboard = rememberCloseKeyboard(editor)
    ErrorText(inputData.emailError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = translate("formLabels.email"),
        value = inputData.email,
        onValueChange = updateEmail,
        keyboardType = KeyboardType.Email,
        isError = isEmailError,
        hasFocus = focusEmail,
        onNext = clearEmailError,
        enabled = isEditable,
        imeAction = ImeAction.Done,
        onEnter = closeKeyboard
    )

    val autoContactFrequencyLabel = translate("casesVue.auto_contact_frequency")
    WithHelpDialog(
        viewModel,
        helpTitle = autoContactFrequencyLabel,
        helpText = translate("casesVue.auto_contact_frequency_help"),
        okText = translate("actions.ok"),
    ) { showHelp ->
        HelpRow(autoContactFrequencyLabel, viewModel.helpHint, showHelp = showHelp)
    }
    val updateContactFrequency = remember(inputData) {
        { it: AutoContactFrequency -> inputData.autoContactFrequency = it }
    }
    val contactFrequencyOptions by editor.contactFrequencyOptions.collectAsStateWithLifecycle()
    ErrorText(inputData.frequencyError)
    Column(modifier = Modifier.selectableGroup()) {
        contactFrequencyOptions.forEach {
            val isSelected = inputData.autoContactFrequency == it.first
            Row(
                Modifier
                    .fillMaxWidth()
                    .listItemHeight()
                    .selectable(
                        selected = isSelected,
                        onClick = { updateContactFrequency(it.first) },
                        role = Role.RadioButton,
                    )
                    .listItemPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    modifier = Modifier.listItemNestedPadding(),
                    onClick = null,
                    enabled = isEditable,
                )
                Text(
                    text = it.second,
                    modifier = Modifier.listItemNestedPadding(),
                )
            }
        }
    }
}

@Composable
private fun PropertyFormResidentNameView(
    editor: CasePropertyDataEditor,
    isEditable: Boolean = true,
    focusOnOpen: Boolean = false,
    translate: (String) -> String = { s -> s },
) {
    val inputData = editor.propertyInputData

    val residentName by inputData.residentName.collectAsStateWithLifecycle()
    val updateName = remember(inputData) { { s: String -> inputData.residentName.value = s } }
    val clearNameError = remember(inputData) { { inputData.residentNameError = "" } }
    val isNameError = inputData.residentNameError.isNotEmpty()
    val focusName = (focusOnOpen && residentName.isEmpty()) || isNameError
    ErrorText(inputData.residentNameError)
    Box(Modifier.fillMaxWidth()) {
        var contentWidth by remember { mutableStateOf(Size.Zero) }

        val nameLabel = translate("formLabels.name")
        OutlinedClearableTextField(
            modifier = listItemModifier.onGloballyPositioned {
                contentWidth = it.size.toSize()
            },
            labelResId = 0,
            label = "$nameLabel *",
            value = residentName,
            onValueChange = updateName,
            keyboardType = KeyboardType.Text,
            isError = isNameError,
            hasFocus = focusName,
            onNext = clearNameError,
            enabled = isEditable,
        )

        val existingCasesResults by editor.searchResults.collectAsStateWithLifecycle()

        val onCaseSelect = remember(editor) {
            { caseLocation: CaseSummaryResult ->
                editor.onExistingWorksiteSelected(caseLocation)
            }
        }

        var hideDropdown by remember { mutableStateOf(false) }
        val onStopSuggestions = remember(editor) {
            {
                hideDropdown = true
                editor.stopSearchingWorksites()
            }
        }

        if (!(hideDropdown || existingCasesResults.isEmpty)) {
            DropdownMenu(
                modifier = Modifier
                    .width(with(LocalDensity.current) { contentWidth.width.toDp() }),
                expanded = true,
                onDismissRequest = onStopSuggestions,
                offset = listItemDropdownMenuOffset,
                properties = PopupProperties(focusable = false)
            ) {
                // TODO Add same option in location search dropdown.
                //      Stop searching entirely for the editing session when selected.
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.stop_suggesting_existing_cases),
                            modifier = Modifier.offset(x = 12.dp),
                        )
                    },
                    onClick = onStopSuggestions,
                )
                ExistingCaseLocationsDropdownItems(
                    existingCasesResults.worksites,
                    onCaseSelect,
                )
            }
        }
    }
}