package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCasePropertyViewModel
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun PropertySummaryView(
    worksite: Worksite,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
) {
    Column(
        modifier
            .padding(16.dp)
            .clickable(onClick = onEdit)
    ) {
        Text(
            text = stringResource(R.string.property_information),
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
        )

        if (worksite.name.isNotEmpty()) {
            Text(
                text = worksite.name,
                modifier = modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCasePropertyRoute(
    viewModel: EditCasePropertyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    BackHandler {
        // TODO Save or check changes before backing out
        onBackClick()
    }

    // TODO Reset to false every time a new screen opens
    val navigateBack by remember { viewModel.navigateBack }
    if (navigateBack) {
        onBackClick()
    } else {
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
            PropertyFormView()
        }
    }
}

@Composable
internal fun PropertyFormView(
    modifier: Modifier = Modifier,
    viewModel: EditCasePropertyViewModel = hiltViewModel(),
) {
    // TODO Make function/constant
    val columnItemModifier = modifier
        .fillMaxWidth()
        .padding(16.dp, 8.dp)

    val propertyInputData = viewModel.propertyInputData

    val updateName = remember(propertyInputData) {
        { it: String -> propertyInputData.residentName = it }
    }
    val clearNameError = remember(propertyInputData) {
        { propertyInputData.residentNameError = "" }
    }
    val isNameError = propertyInputData.residentNameError.isNotEmpty()
    val focusName = propertyInputData.residentName.isEmpty() || isNameError
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedClearableTextField(
            modifier = columnItemModifier,
            labelResId = R.string.resident_name,
            value = propertyInputData.residentName,
            onValueChange = { updateName(it) },
            keyboardType = KeyboardType.Text,
            isError = isNameError,
            hasFocus = focusName,
            onNext = clearNameError,
            enabled = true,
        )

        // TODO Apply mask with dashes if input is purely numbers (and dashes)
        val updatePhone = remember(propertyInputData) {
            { it: String -> propertyInputData.phoneNumber = it }
        }
        val clearPhoneError = remember(propertyInputData) {
            { propertyInputData.phoneNumberError = "" }
        }
        val isPhoneError = propertyInputData.phoneNumberError.isNotEmpty()
        val focusPhone = isPhoneError
        OutlinedClearableTextField(
            modifier = columnItemModifier,
            labelResId = R.string.phone_number,
            value = propertyInputData.phoneNumber,
            onValueChange = { updatePhone(it) },
            keyboardType = KeyboardType.Phone,
            isError = isPhoneError,
            hasFocus = focusPhone,
            onNext = clearPhoneError,
            enabled = true,
        )

        val updateAdditionalPhone = remember(propertyInputData) {
            { it: String -> propertyInputData.phoneNumberSecondary = it }
        }
        val clearAdditionalPhoneError = remember(propertyInputData) {
            { propertyInputData.phoneNumberError = "" }
        }
        val isAdditionalPhoneError = propertyInputData.phoneNumberError.isNotEmpty()
        val focusAdditionalPhone = isPhoneError
        OutlinedClearableTextField(
            modifier = columnItemModifier,
            labelResId = R.string.additional_phone_number,
            value = propertyInputData.phoneNumberSecondary,
            onValueChange = { updateAdditionalPhone(it) },
            keyboardType = KeyboardType.Text,
            isError = isAdditionalPhoneError,
            hasFocus = focusAdditionalPhone,
            onNext = clearAdditionalPhoneError,
            enabled = true,
        )

        val updateEmail = remember(propertyInputData) {
            { it: String -> propertyInputData.email = it }
        }
        val clearEmailError = remember(propertyInputData) {
            { propertyInputData.emailError = "" }
        }
        val isEmailError = propertyInputData.emailError.isNotEmpty()
        val focusEmail = isEmailError
        OutlinedClearableTextField(
            modifier = columnItemModifier,
            labelResId = R.string.email_address,
            value = propertyInputData.email,
            onValueChange = { updateEmail(it) },
            keyboardType = KeyboardType.Email,
            isError = isEmailError,
            hasFocus = focusEmail,
            onNext = clearEmailError,
            enabled = true,
        )

        // TODO Help action showing help text. May be provided in form fields.
        Text(
            text = stringResource(R.string.auto_contact_frequency),
            modifier = columnItemModifier,
        )
        val autoContactOptions by viewModel.autoContactOptions.collectAsState()
        val updateAutoContact = remember(propertyInputData) {
            { it: AutoContactFrequency -> propertyInputData.autoContactFrequency = it }
        }
        Column(modifier = Modifier.selectableGroup()) {
            autoContactOptions.forEach {
                val isSelected = propertyInputData.autoContactFrequency == it.first
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { updateAutoContact(it.first) },
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
}