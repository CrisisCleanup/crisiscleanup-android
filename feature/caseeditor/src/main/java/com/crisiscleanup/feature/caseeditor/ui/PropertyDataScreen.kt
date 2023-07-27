package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.commoncase.ui.ExistingCaseLocationsDropdownItems
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.HelpRow
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.WithHelpDialog
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemDropdownMenuOffset
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.feature.caseeditor.CasePropertyDataEditor
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel

@Composable
internal fun PropertyFormView(
    viewModel: EditCaseBaseViewModel,
    editor: CasePropertyDataEditor,
    focusOnOpen: Boolean = false,
) {
    val translator = LocalAppTranslator.current
    val isEditable = LocalCaseEditor.current.isEditable

    val inputData = editor.propertyInputData

    PropertyFormResidentNameView(editor, isEditable, focusOnOpen)

    // TODO Apply mask with dashes if input is purely numbers (and dashes)
    val updatePhone = remember(inputData) { { s: String -> inputData.phoneNumber = s } }
    val clearPhoneError = remember(inputData) { { inputData.phoneNumberError = "" } }
    val isPhoneError = inputData.phoneNumberError.isNotEmpty()
    val focusPhone = isPhoneError
    val phone1Label = translator("formLabels.phone1")
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
        label = translator("formLabels.phone2"),
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
        label = translator("formLabels.email"),
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

    val autoContactFrequencyLabel = translator("casesVue.auto_contact_frequency")
    WithHelpDialog(
        viewModel,
        helpTitle = autoContactFrequencyLabel,
        helpText = translator("casesVue.auto_contact_frequency_help"),
    ) { showHelp ->
        HelpRow(
            autoContactFrequencyLabel,
            viewModel.helpHint,
            Modifier
                .listItemHorizontalPadding()
                // TODO Common dimensions
                .padding(top = 16.dp),
            showHelp = showHelp
        )
    }
    val updateContactFrequency = remember(inputData) {
        { it: AutoContactFrequency -> inputData.autoContactFrequency = it }
    }
    val contactFrequencyOptions by editor.contactFrequencyOptions.collectAsStateWithLifecycle()
    Column(modifier = Modifier.selectableGroup()) {
        contactFrequencyOptions.forEach {
            val isSelected = inputData.autoContactFrequency == it.first
            CrisisCleanupRadioButton(
                Modifier
                    .fillMaxWidth()
                    .listItemHeight()
                    .listItemPadding(),
                isSelected,
                text = it.second,
                onSelect = { updateContactFrequency(it.first) },
                enabled = isEditable,
            )
        }
    }
}

@Composable
private fun PropertyFormResidentNameView(
    editor: CasePropertyDataEditor,
    isEditable: Boolean = true,
    focusOnOpen: Boolean = false,
) {
    val translator = LocalAppTranslator.current
    val inputData = editor.propertyInputData

    val residentName by inputData.residentName.collectAsStateWithLifecycle()
    val updateName = remember(inputData) { { s: String -> inputData.residentName.value = s } }
    val clearNameError = remember(inputData) { { inputData.residentNameError = "" } }
    val isNameError = inputData.residentNameError.isNotEmpty()
    val focusName = (focusOnOpen && residentName.isEmpty()) || isNameError
    ErrorText(inputData.residentNameError)
    Box(Modifier.fillMaxWidth()) {
        var contentWidth by remember { mutableStateOf(Size.Zero) }

        val nameLabel = translator("formLabels.name")
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

        if (!existingCasesResults.isEmpty) {
            var hideDropdown by remember { mutableStateOf(false) }
            val onStopSuggestions = remember(editor) {
                {
                    hideDropdown = true
                    editor.stopSearchingWorksites()
                }
            }

            DropdownMenu(
                modifier = Modifier
                    .width(with(LocalDensity.current) { contentWidth.width.toDp() }),
                expanded = !hideDropdown,
                onDismissRequest = onStopSuggestions,
                offset = listItemDropdownMenuOffset,
                properties = PopupProperties(focusable = false)
            ) {
                // TODO Add same option in location search dropdown.
                //      Stop searching entirely for the editing session when selected.
                DropdownMenuItem(
                    text = {
                        Text(
                            LocalAppTranslator.current("actions.stop_searching_cases"),
                            modifier = Modifier.offset(x = 12.dp),
                            style = LocalFontStyles.current.header4,
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