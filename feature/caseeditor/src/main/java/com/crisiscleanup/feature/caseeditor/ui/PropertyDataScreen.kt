package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.commoncase.ui.ExistingCaseLocationsDropdownItems
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.theme.*
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.EditCasePropertyViewModel
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.R

private const val ScreenTitleTranslateKey = "caseForm.property_information"

@Composable
internal fun PropertySummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    translate: (String) -> String = { s -> s },
) {
    EditCaseSummaryHeader(
        0,
        isEditable,
        onEdit,
        modifier,
        translate(ScreenTitleTranslateKey),
    ) {
        val texts = listOf(
            worksite.name,
            worksite.phone1,
            worksite.phone2,
            worksite.email,
        ).filterNotBlankTrim()
        texts.forEach {
            Text(
                text = it,
                modifier = modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
internal fun EditCasePropertyRoute(
    viewModel: EditCasePropertyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    openExistingCase: (ids: ExistingWorksiteIdentifier) -> Unit = { _ -> },
) {
    val editDifferentWorksite by viewModel.editIncidentWorksite.collectAsStateWithLifecycle()
    if (editDifferentWorksite.isDefined) {
        openExistingCase(editDifferentWorksite)
    } else {
        EditCasePropertyView(onBackClick = onBackClick)
    }
}

@Composable
private fun EditCasePropertyView(
    viewModel: EditCasePropertyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    EditCaseBackCancelView(
        viewModel,
        onBackClick,
        viewModel.translate(ScreenTitleTranslateKey)
    ) {

        val closeKeyboard = rememberCloseKeyboard(viewModel)
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .scrollFlingListener(closeKeyboard)
                .verticalScroll(scrollState)
                .weight(1f)
        ) {
            PropertyFormView()
        }
    }
}

@Composable
private fun PropertyFormView(
    viewModel: EditCasePropertyViewModel = hiltViewModel(),
) {
    val inputData = viewModel.propertyInputData

    PropertyFormResidentNameView()

    // TODO Apply mask with dashes if input is purely numbers (and dashes)
    val updatePhone = remember(inputData) { { s: String -> inputData.phoneNumber = s } }
    val clearPhoneError = remember(inputData) { { inputData.phoneNumberError = "" } }
    val isPhoneError = inputData.phoneNumberError.isNotEmpty()
    val focusPhone = isPhoneError
    ErrorText(inputData.phoneNumberError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("formLabels.phone1"),
        value = inputData.phoneNumber,
        onValueChange = updatePhone,
        keyboardType = KeyboardType.Password,
        isError = isPhoneError,
        hasFocus = focusPhone,
        onNext = clearPhoneError,
        enabled = true,
    )

    val updateAdditionalPhone = remember(inputData) {
        { s: String -> inputData.phoneNumberSecondary = s }
    }
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("formLabels.phone2"),
        value = inputData.phoneNumberSecondary,
        onValueChange = updateAdditionalPhone,
        keyboardType = KeyboardType.Password,
        isError = false,
        enabled = true,
    )

    val updateEmail = remember(inputData) { { s: String -> inputData.email = s } }
    val clearEmailError = remember(inputData) { { inputData.emailError = "" } }
    val isEmailError = inputData.emailError.isNotEmpty()
    val focusEmail = isEmailError
    val closeKeyboard = rememberCloseKeyboard(viewModel)
    ErrorText(inputData.emailError)
    OutlinedClearableTextField(
        modifier = listItemModifier,
        labelResId = 0,
        label = viewModel.translate("formLabels.email"),
        value = inputData.email,
        onValueChange = updateEmail,
        keyboardType = KeyboardType.Email,
        isError = isEmailError,
        hasFocus = focusEmail,
        onNext = clearEmailError,
        enabled = true,
        imeAction = ImeAction.Done,
        onEnter = closeKeyboard
    )

    val autoContactFrequencyLabel = viewModel.translate("casesVue.auto_contact_frequency")

    var helpTitle by remember { mutableStateOf("") }
    var helpText by remember { mutableStateOf("") }
    val showHelp = remember(viewModel) {
        {
            helpTitle = autoContactFrequencyLabel
            helpText = viewModel.translate("casesVue.auto_contact_frequency_help")
        }
    }
    Row(
        modifier = listItemModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = autoContactFrequencyLabel)
        HelpAction(viewModel.helpHint, showHelp)
    }
    val updateContactFrequency = remember(inputData) {
        { it: AutoContactFrequency -> inputData.autoContactFrequency = it }
    }
    val contactFrequencyOptions by viewModel.contactFrequencyOptions.collectAsState()
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
                    modifier = Modifier.listRowItemStartPadding(),
                    onClick = null,
                )
                Text(
                    text = it.second,
                    modifier = Modifier.listRowItemStartPadding(),
                )
            }
        }
    }

    if (helpText.isNotBlank()) {
        HelpDialog(
            title = helpTitle,
            text = helpText,
            onClose = { helpText = "" },
        )
    }
}

@Composable
private fun PropertyFormResidentNameView(
    viewModel: EditCasePropertyViewModel = hiltViewModel(),
) {
    val inputData = viewModel.propertyInputData

    val residentName by inputData.residentName.collectAsStateWithLifecycle()
    val updateName = remember(inputData) { { s: String -> inputData.residentName.value = s } }
    val clearNameError = remember(inputData) { { inputData.residentNameError = "" } }
    val isNameError = inputData.residentNameError.isNotEmpty()
    val focusName = residentName.isEmpty() || isNameError
    ErrorText(inputData.residentNameError)
    Box(Modifier.fillMaxWidth()) {
        var contentWidth by remember { mutableStateOf(Size.Zero) }

        OutlinedClearableTextField(
            modifier = listItemModifier.onGloballyPositioned {
                contentWidth = it.size.toSize()
            },
            labelResId = 0,
            label = viewModel.translate("formLabels.name"),
            value = residentName,
            onValueChange = updateName,
            keyboardType = KeyboardType.Text,
            isError = isNameError,
            hasFocus = focusName,
            onNext = clearNameError,
            enabled = true,
        )

        val existingCasesResults by viewModel.searchResults.collectAsStateWithLifecycle()

        val onCaseSelect = remember(viewModel) {
            { caseLocation: CaseSummaryResult ->
                viewModel.onExistingWorksiteSelected(caseLocation)
            }
        }

        var hideDropdown by remember { mutableStateOf(false) }
        val onStopSuggestions = remember(viewModel) {
            {
                hideDropdown = true
                viewModel.stopSearchingWorksites()
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