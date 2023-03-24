package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.EditCasePropertyViewModel
import com.crisiscleanup.feature.caseeditor.ExistingWorksiteIdentifier
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.model.ExistingCaseLocation
import com.crisiscleanup.feature.caseeditor.util.filterNotBlankTrim

@Composable
internal fun PropertySummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
) {
    EditCaseSummaryHeader(
        R.string.property_information,
        isEditable,
        onEdit,
        modifier,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCasePropertyView(
    viewModel: EditCasePropertyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    BackHandler {
        if (viewModel.onSystemBack()) {
            onBackClick()
        }
    }

    val onNavigateBack = remember(viewModel) {
        {
            if (viewModel.onNavigateBack()) {
                onBackClick()
            }
        }
    }
    val onNavigateCancel = remember(viewModel) {
        {
            if (viewModel.onNavigateCancel()) {
                onBackClick()
            }
        }
    }
    Column {
        TopAppBarBackCancel(
            titleResId = R.string.property_information,
            onBack = onNavigateBack,
            onCancel = onNavigateCancel,
        )

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
    val propertyInputData = viewModel.propertyInputData

    PropertyFormResidentNameView()

    // TODO Apply mask with dashes if input is purely numbers (and dashes)
    val updatePhone =
        remember(propertyInputData) { { s: String -> propertyInputData.phoneNumber = s } }
    val clearPhoneError =
        remember(propertyInputData) { { propertyInputData.phoneNumberError = "" } }
    val isPhoneError = propertyInputData.phoneNumberError.isNotEmpty()
    val focusPhone = isPhoneError
    ErrorText(propertyInputData.phoneNumberError)
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.phone_number,
        value = propertyInputData.phoneNumber,
        onValueChange = updatePhone,
        keyboardType = KeyboardType.Password,
        isError = isPhoneError,
        hasFocus = focusPhone,
        onNext = clearPhoneError,
        enabled = true,
    )

    val updateAdditionalPhone = remember(propertyInputData) {
        { s: String -> propertyInputData.phoneNumberSecondary = s }
    }
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.additional_phone_number,
        value = propertyInputData.phoneNumberSecondary,
        onValueChange = updateAdditionalPhone,
        keyboardType = KeyboardType.Password,
        isError = false,
        enabled = true,
    )

    val updateEmail =
        remember(propertyInputData) { { s: String -> propertyInputData.email = s } }
    val clearEmailError = remember(propertyInputData) { { propertyInputData.emailError = "" } }
    val isEmailError = propertyInputData.emailError.isNotEmpty()
    val focusEmail = isEmailError
    val closeKeyboard = rememberCloseKeyboard(viewModel)
    ErrorText(propertyInputData.emailError)
    OutlinedClearableTextField(
        modifier = columnItemModifier,
        labelResId = R.string.email_address,
        value = propertyInputData.email,
        onValueChange = updateEmail,
        keyboardType = KeyboardType.Email,
        isError = isEmailError,
        hasFocus = focusEmail,
        onNext = clearEmailError,
        enabled = true,
        imeAction = ImeAction.Done,
        onEnter = closeKeyboard
    )

    // TODO Help action showing help text.
    Text(
        text = stringResource(R.string.auto_contact_frequency),
        modifier = columnItemModifier,
    )
    val updateContactFrequency = remember(propertyInputData) {
        { it: AutoContactFrequency -> propertyInputData.autoContactFrequency = it }
    }
    val contactFrequencyOptions by viewModel.contactFrequencyOptions.collectAsState()
    ErrorText(propertyInputData.frequencyError)
    Column(modifier = Modifier.selectableGroup()) {
        contactFrequencyOptions.forEach {
            val isSelected = propertyInputData.autoContactFrequency == it.first
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = isSelected,
                        onClick = { updateContactFrequency(it.first) },
                        role = Role.RadioButton,
                    )
                    // TODO Match padding to rest of items
                    .padding(16.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                )
                Text(
                    text = it.second,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun PropertyFormResidentNameView(
    viewModel: EditCasePropertyViewModel = hiltViewModel(),
) {
    val propertyInputData = viewModel.propertyInputData

    val residentName by propertyInputData.residentName.collectAsStateWithLifecycle()
    val updateName =
        remember(propertyInputData) { { s: String -> propertyInputData.residentName.value = s } }
    val clearNameError =
        remember(propertyInputData) { { propertyInputData.residentNameError = "" } }
    val isNameError = propertyInputData.residentNameError.isNotEmpty()
    val focusName = residentName.isEmpty() || isNameError
    ErrorText(propertyInputData.residentNameError)
    Box(Modifier.fillMaxWidth()) {
        var contentWidth by remember { mutableStateOf(Size.Zero) }

        OutlinedClearableTextField(
            modifier = columnItemModifier.onGloballyPositioned {
                contentWidth = it.size.toSize()
            },
            labelResId = R.string.resident_name,
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
            { caseLocation: ExistingCaseLocation ->
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
                // TODO Use common styles
                offset = DpOffset(16.dp, 0.dp),
                properties = PopupProperties(focusable = false)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.stop_suggesting_existing_cases),
                            // TODO Use common styles
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
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