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
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.property_information),
            style = MaterialTheme.typography.headlineSmall,
        )

        if (worksite.name.isNotEmpty()) {
            Column(modifier.padding(8.dp)) {
                Text(
                    text = worksite.name,
                    modifier = modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
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
        if (viewModel.onSystemBack()) {
            onBackClick()
        }
    }

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

// TODO Move into util/common
@Composable
internal fun ErrorText(
    errorMessage: String,
) {
    if (errorMessage.isNotEmpty()) {
        Text(
            errorMessage,
            modifier = errorMessageModifier,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private val errorMessageModifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)

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
        ErrorText(propertyInputData.residentNameError)
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
        ErrorText(propertyInputData.phoneNumberError)
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
        OutlinedClearableTextField(
            modifier = columnItemModifier,
            labelResId = R.string.additional_phone_number,
            value = propertyInputData.phoneNumberSecondary,
            onValueChange = { updateAdditionalPhone(it) },
            keyboardType = KeyboardType.Text,
            isError = false,
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
        ErrorText(propertyInputData.emailError)
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
}